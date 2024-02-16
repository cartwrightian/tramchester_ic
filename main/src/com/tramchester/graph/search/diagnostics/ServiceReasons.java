package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.RouteCalculatorSupport;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ServiceReasons {

    private static final Logger logger;
    public static final int NUMBER_MOST_VISITED_NODES_TO_LOG = 10;
//    public static final int THRESHHOLD_FOR_NUMBER_VISITS_DIAGS = 400;

    static {
        logger = LoggerFactory.getLogger(ServiceReasons.class);
    }

    private final TramTime queryTime;
    private final ProvidesNow providesLocalNow;
    private final JourneyRequest journeyRequest;
    private final List<HeuristicsReason> reasons;
    // stats
    private final EnumMap<ReasonCode, AtomicInteger> reasonCodeStats; // reason -> count
    private final EnumMap<TraversalStateType, AtomicInteger> stateStats; // State -> num visits
    private final Map<GraphNodeId, AtomicInteger> nodeVisits; // count of visits to nodes
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final boolean diagnosticsEnabled;

    private boolean success;

    public ServiceReasons(JourneyRequest journeyRequest, TramTime queryTime, ProvidesNow providesLocalNow) {
        this.queryTime = queryTime;
        this.providesLocalNow = providesLocalNow;
        this.journeyRequest = journeyRequest;
        reasons = new ArrayList<>();
        success = false;
        diagnosticsEnabled = journeyRequest.getDiagnosticsEnabled();

        reasonCodeStats = new EnumMap<>(ReasonCode.class);
        Arrays.asList(ReasonCode.values()).forEach(code -> reasonCodeStats.put(code, new AtomicInteger(0)));

        stateStats = new EnumMap<>(TraversalStateType.class);
        nodeVisits = new HashMap<>();
    }

    private void reset() {
        reasons.clear();
        reasonCodeStats.clear();
        stateStats.clear();
        nodeVisits.clear();
        Arrays.asList(ReasonCode.values()).forEach(code -> reasonCodeStats.put(code, new AtomicInteger(0)));
    }

    public void reportReasons(final GraphTransaction transaction, final RouteCalculatorSupport.PathRequest pathRequest, final ReasonsToGraphViz reasonToGraphViz) {
        if (diagnosticsEnabled) {
            createGraphFile(transaction, reasonToGraphViz, pathRequest);
        }

        if (!success || diagnosticsEnabled) {
            reportStats(transaction, pathRequest);
        }

        reset();
    }


    public HeuristicsReason recordReason(final HeuristicsReason serviceReason) {
        if (diagnosticsEnabled) {
            reasons.add(serviceReason);
            recordEndNodeVisit(serviceReason.getHowIGotHere());
        } else {
            if (!serviceReason.isValid()) {
                recordEndNodeVisit(serviceReason.getHowIGotHere());
            }
        }

        incrementStat(serviceReason.getReasonCode());
        return serviceReason;
    }

    private void recordEndNodeVisit(final HowIGotHere howIGotHere) {
        final GraphNodeId endNode = howIGotHere.getEndNodeId();
        if (nodeVisits.containsKey(endNode)) {
            nodeVisits.get(endNode).incrementAndGet();
        } else {
            nodeVisits.put(endNode, new AtomicInteger(1));
        }
    }

    public void incrementTotalChecked() {
        totalChecked.incrementAndGet();
    }

    private void incrementStat(final ReasonCode reasonCode) {
        reasonCodeStats.get(reasonCode).incrementAndGet();
    }

    public void recordSuccess() {
        incrementStat(ReasonCode.Arrived);
        success = true;
    }

    public void recordStat(final ImmutableJourneyState journeyState) {
        final ReasonCode reason = getReasonCode(journeyState.getTransportMode());
        incrementStat(reason);

        final TraversalStateType stateType = journeyState.getTraversalStateType();
        if (stateStats.containsKey(stateType)) {
            stateStats.get(stateType).incrementAndGet();
        } else {
            stateStats.put(stateType, new AtomicInteger(1));
        }
    }

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

    private void reportStats(final GraphTransaction txn, final RouteCalculatorSupport.PathRequest pathRequest) {
        if ((!success) && journeyRequest.getWarnIfNoResults()) {
            logger.warn("No result found for at " + pathRequest.getActualQueryTime() + " changes " + pathRequest.getNumChanges() +
                    " for " + journeyRequest );
        }
        logger.info("Service reasons for query time: " + queryTime);
        logger.info("Total checked: " + totalChecked.get() + " for " + journeyRequest.toString());
        logStats("reasoncodes", reasonCodeStats);
        logStats("states", stateStats);
        if (diagnosticsEnabled) {
            logVisits(txn);
        }
    }

    private void logVisits(final GraphTransaction txn) {
        final Set<GraphNodeId> haveInvalidReasonCode = reasons.stream().
                filter(reason -> !reason.isValid()).
                map(HeuristicsReason::getNodeId).
                collect(Collectors.toSet());

        // Pair<Node, Number of Visits>
        final Set<Pair<ImmutableGraphNode, Integer>> topVisits = nodeVisits.entrySet().stream().
                filter(entry -> haveInvalidReasonCode.contains(entry.getKey())).
                map(entry -> Pair.of(entry.getKey(), entry.getValue().get())).
                //filter(entry -> entry.getValue() > THRESHHOLD_FOR_NUMBER_VISITS_DIAGS).
                sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                limit(NUMBER_MOST_VISITED_NODES_TO_LOG).
                map(entry -> Pair.of(txn.getNodeById(entry.getKey()), entry.getValue())).
                collect(Collectors.toSet());

         topVisits.stream().map(pair -> Pair.of(nodeDetails(pair.getKey()), pair.getValue())).
                forEach(entry -> logger.info("Visited " + entry.getKey() + " " + entry.getValue() + " times"));

         topVisits.stream().map(Pair::getKey).forEach(this::reasonsAtNode);

    }

    private void reasonsAtNode(final GraphNode node) {
        final GraphNodeId nodeId = node.getId();
        // beware of Set here, will collapse reasons
        List<HeuristicsReason> reasonsForId = reasons.stream().
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

    private void logStats(final String prefix, final Map<?, AtomicInteger> stats) {
        stats.entrySet().stream().
                filter(entry -> entry.getValue().get() > 0).
                sorted(Comparator.comparingInt(a -> a.getValue().get())).
                forEach(entry -> logger.info(format("%s => %s: %s", prefix, entry.getKey(), entry.getValue().get())));
    }

    private void createGraphFile(final GraphTransaction txn, final ReasonsToGraphViz reasonsToGraphViz, final RouteCalculatorSupport.PathRequest pathRequest) {
        final String fileName = createFilename(pathRequest);

        if (reasons.isEmpty()) {
            logger.warn(format("Not creating dot file %s, reasons empty", fileName));
            return;
        } else {
            logger.warn("Creating diagnostic dot file: " + fileName);
        }

        try {
            final StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");
            reasonsToGraphViz.appendTo(builder, reasons, txn);
            builder.append("}");

            final FileWriter writer = new FileWriter(fileName);
            writer.write(builder.toString());
            writer.close();
            logger.info(format("Created file %s", fileName));
        }
        catch (IOException e) {
            logger.warn("Unable to create diagnostic graph file", e);
        }
    }

    private String createFilename(final RouteCalculatorSupport.PathRequest pathRequest) {
        final String status = success ? "found" : "notfound";
        final String dateString = providesLocalNow.getDateTime().toLocalDate().toString();
        final String changes = "changes" + pathRequest.getNumChanges();
        final String postfix = journeyRequest.getUid().toString();

        String fileName = format("%s_%s%s_at_%s_%s_%s.dot", status,
                queryTime.getHourOfDay(), queryTime.getMinuteOfHour(),
                dateString, changes, postfix);
        fileName = fileName.replaceAll(":","");
        return fileName;
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
}
