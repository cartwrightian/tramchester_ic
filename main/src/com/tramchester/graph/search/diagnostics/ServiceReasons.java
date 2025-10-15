package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.PathRequest;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ServiceReasons {

    private static final Logger logger;
    public static final int NUMBER_MOST_VISITED_NODES_TO_LOG = 10;

    static {
        logger = LoggerFactory.getLogger(ServiceReasons.class);
    }

    private final TramTime queryTime;
    private final ProvidesNow providesLocalNow;
    private final JourneyRequest journeyRequest;
    private final CreateJourneyDiagnostics failedJourneyDiagnostics;

    private final List<HeuristicsReason> reasons;
    // stats
    private final EnumCounter<ReasonCode> reasonCodeStats; // reason -> count
    private final EnumCounter<TraversalStateType> stateStats; // State -> num visits
    private final Map<GraphNodeId, AtomicInteger> nodeVisits; // count of visits to nodes
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final boolean diagnosticsEnabled;

    private final AtomicBoolean success;

    public ServiceReasons(final JourneyRequest journeyRequest, final TramTime queryTime, final ProvidesNow providesLocalNow,
                          final CreateJourneyDiagnostics failedJourneyDiagnostics) {
        this.queryTime = queryTime;
        this.providesLocalNow = providesLocalNow;
        this.journeyRequest = journeyRequest;
        this.failedJourneyDiagnostics = failedJourneyDiagnostics;
        reasons = new ArrayList<>();
        success = new AtomicBoolean(false);
        diagnosticsEnabled = journeyRequest.getDiagnosticsEnabled();

        reasonCodeStats = new EnumCounter<>(ReasonCode.class);
        stateStats = new EnumCounter<>(TraversalStateType.class);

        nodeVisits = new HashMap<>();
    }

    public void reportReasons(final GraphTransaction txn, final PathRequest pathRequest, final LocationCollection destinations) {
        if (diagnosticsEnabled) {
            final JourneyDiagnostics diagnostics = failedJourneyDiagnostics.recordFailedJourneys(reasons, destinations);
            journeyRequest.injectDiag(diagnostics);
        }

        if (!success.get() || diagnosticsEnabled) {
            reportStats(txn, pathRequest);
        }

        reset();
    }

    public void recordVisit(final HowIGotHere howIGotHere) {
        if (diagnosticsEnabled) {
            recordEndNodeVisit(howIGotHere);
        }
    }

    private void reset() {
        reasons.clear();
        nodeVisits.clear();
        reasonCodeStats.clear();
        stateStats.clear();
    }

    public HeuristicsReason recordReason(final HeuristicsReason serviceReason) {

        final ReasonCode reasonCode = serviceReason.getReasonCode();

        if (reasonCode ==ReasonCode.Arrived) {
            success.set(true);
        }

        if (diagnosticsEnabled) {
            addReason(serviceReason);
        }

        incrementReasonCode(reasonCode);
        return serviceReason;
    }

    public void incrementTotalChecked() {
        totalChecked.incrementAndGet();
    }

    public void recordState(final ImmutableJourneyState journeyState) {
        final ReasonCode reason = getReasonCode(journeyState.getTransportMode());
        incrementReasonCode(reason);

        final TraversalStateType stateType = journeyState.getTraversalStateType();
        recordStateType(stateType);
    }

    //*********** Safe access to counters

    private synchronized void addReason(final HeuristicsReason serviceReason) {
        reasons.add(serviceReason);
    }

    private synchronized void recordEndNodeVisit(final HowIGotHere howIGotHere) {
        final GraphNodeId endNodeId = howIGotHere.getEndNodeId();

        if (nodeVisits.containsKey(endNodeId)) {
            nodeVisits.get(endNodeId).getAndIncrement();
        } else {
            nodeVisits.put(endNodeId, new AtomicInteger(1));
        }
    }

    private void recordStateType(final TraversalStateType stateType) {
        stateStats.increment(stateType);
    }

    private void incrementReasonCode(final ReasonCode reasonCode) {
        reasonCodeStats.increment(reasonCode);
    }

    //***********

    private ReasonCode getReasonCode(final TransportMode transportMode) {
        return switch (transportMode) {
            case Tram -> ReasonCode.OnTram;
            case Bus, RailReplacementBus -> ReasonCode.OnBus;
            case Train -> ReasonCode.OnTrain;
            case Walk, Connect -> ReasonCode.OnWalk;
            case Ferry, Ship -> ReasonCode.OnShip;
            case Subway -> ReasonCode.OnSubway;
            case NotSet -> ReasonCode.NotOnVehicle;
            case Unknown -> throw new RuntimeException("Unknown transport mode");
        };
    }

    private void reportStats(final GraphTransaction txn, final PathRequest pathRequest) {
        int numberOfChanges = pathRequest.getNumChanges();
        TramTime actualTime = pathRequest.getActualQueryTime();
        if ((!success.get()) && journeyRequest.getWarnIfNoResults()) {
            logger.warn("No result found at " + actualTime + " changes " + numberOfChanges + " for " + journeyRequest );
        }
        logger.info("Service reasons for query time: " + queryTime);
        logCounters();
        if (diagnosticsEnabled) {
            logVisits(txn);
        }
    }

    public void logCounters() {
        logger.info("Total checked: " + totalChecked.get() + " for " + journeyRequest.toString());
        reasonCodeStats.log(logger);
        stateStats.log(logger);
        if (diagnosticsEnabled) {
            logger.info("Visited " + nodeVisits.size() + " nodes");
        }
    }

    private void logVisits(final GraphTransaction txn) {
        final Set<GraphNodeId> haveInvalidReasonCode = reasons.stream().
                filter(reason -> !reason.isValid()).
                map(HeuristicsReason::getNodeId).
                collect(Collectors.toSet());

        // Pair<Node, Number of Visits>
        final List<Pair<GraphNode, Integer>> topVisits = nodeVisits.entrySet().stream().
                filter(entry -> haveInvalidReasonCode.contains(entry.getKey())).
                map(entry -> Pair.of(entry.getKey(), entry.getValue().get())).
                sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                limit(NUMBER_MOST_VISITED_NODES_TO_LOG).
                map(entry -> Pair.of(txn.getNodeById(entry.getKey()), entry.getValue())).
                toList();

         topVisits.stream().map(pair -> Pair.of(nodeDetails(pair.getKey()), pair.getValue())).
                forEach(entry -> logger.info("Visited " + entry.getKey() + " " + entry.getValue() + " times"));

         topVisits.forEach(topVisit -> {
             final GraphNodeId graphNodeId = topVisit.getKey().getId();
             final List<ReasonCode> allReasonsForNode = reasons.stream().
                     filter(reason -> reason.getHowIGotHere().getEndNodeId().equals(graphNodeId)).
                     map(HeuristicsReason::getReasonCode).
                     toList();

             final Map<ReasonCode, Integer> countPerReason = allReasonsForNode.
                     stream().collect(Collectors.toMap(reasonCode -> reasonCode, reason -> 1, Integer::sum));

             logger.info("Reason codes counts for " + nodeDetails(topVisit.getKey()) + " " + countPerReason);

         });

         topVisits.stream().map(Pair::getKey).forEach(this::reasonsAtNode);

    }

    private void reasonsAtNode(final GraphNode node) {
        final GraphNodeId nodeId = node.getId();
        // beware of Set here, will collapse reasons
        final List<HeuristicsReason> reasonsForId = reasons.stream().
                filter(reason -> reason.getNodeId() == nodeId).
                collect(Collectors.toList());
        logger.info("Reasons for node " + nodeDetails(node) + " : " + summaryByCount(reasonsForId));
    }

    private String summaryByCount(final List<HeuristicsReason> reasons) {

        final Map<ReasonCode, AtomicInteger> counts = new HashMap<>();
        Arrays.stream(ReasonCode.values()).forEach(code -> counts.put(code, new AtomicInteger(0)));

        reasons.stream().
                filter(reason -> !reason.isValid()).
                forEach(reason -> counts.get(reason.getReasonCode()).getAndIncrement());

        final StringBuilder stringBuilder = new StringBuilder();
        counts.forEach((key, value) -> {
            if (value.get() > 0) {
                stringBuilder.append(key).append(":").append(value.get()).append(" ");
            }
        });

        counts.clear();
        return stringBuilder.toString();
    }

    private String nodeDetails(final GraphNode node) {
        final StringBuilder labels = new StringBuilder();
        node.getLabels().forEach(label -> labels.append(" ").append(label));
        return labels + " " + node.getAllProperties().toString();
    }

    @Override
    public String toString() {
        return "ServiceReasons{" +
                "queryTime=" + queryTime +
                ", providesLocalNow=" + providesLocalNow +
                ", journeyRequest=" + journeyRequest +
                ", reasons=" + reasons +
                ", reasonCodeStats=" + reasonCodeStats +
                ", stateStats=" + stateStats +
                ", nodeVisits=" + nodeVisits +
                ", totalChecked=" + totalChecked +
                ", diagnosticsEnabled=" + diagnosticsEnabled +
                ", success=" + success +
                '}';
    }

    public int getTotalChecked() {
        return totalChecked.get();
    }

    public Map<ReasonCode, Integer> getReasons() {
        return reasonCodeStats.getCounters();
    }

    public Map<TraversalStateType, Integer> getStates() {
        return stateStats.getCounters();
    }

    public Map<GraphNodeId, Integer> getNodeVisits() {
        return nodeVisits.entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().get()));
    }

    public boolean getDiagnosticsEnabled() {
        return diagnosticsEnabled;
    }

    private static class EnumCounter<T extends Enum<T>> {
        private final EnumMap<T,AtomicInteger> counters;
        private final Class<T> theEnum;

        private EnumCounter(final Class<T> theEnum) {
            this.theEnum = theEnum;
            counters = new EnumMap<>(getInit());
        }

        private Map<T, AtomicInteger> getInit() {
            return Arrays.stream(theEnum.getEnumConstants()).collect(Collectors.toMap(type -> type, type -> new AtomicInteger(0)));
        }

        public void clear() {
            synchronized (counters) {
                counters.clear();
                counters.putAll(getInit());
            }
        }

        public void increment(final T item) {
            synchronized (counters) {
                counters.get(item).getAndIncrement();
            }
        }

        public void log(final Logger logger) {
            getCounters().entrySet().stream().
                    filter(entry -> entry.getValue() > 0).
                    sorted(Comparator.comparingInt(Map.Entry::getValue)).
                    forEach(entry -> logger.info(format("%s => %s: %s", theEnum.getName(), entry.getKey(), entry.getValue())));
        }

        public Map<T, Integer> getCounters() {
            return counters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, item -> item.getValue().get()));
        }

        @Override
        public String toString() {
            return "EnumCounter{" +
                    "counters=" + counters +
                    ", theEnum=" + theEnum +
                    '}';
        }
    }

}
