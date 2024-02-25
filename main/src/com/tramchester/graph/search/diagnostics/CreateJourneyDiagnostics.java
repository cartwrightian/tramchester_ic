package com.tramchester.graph.search.diagnostics;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.diagnostics.DiagnosticReasonDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsLinkDTO;
import com.tramchester.repository.LocationRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class CreateJourneyDiagnostics {
    private static final Logger logger = LoggerFactory.getLogger(CreateJourneyDiagnostics.class);

    private final LocationRepository locationRepository;


    @Inject
    public CreateJourneyDiagnostics(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public JourneyDiagnostics recordFailedJourneys(final List<HeuristicsReason> reasons) {
        final DiagnosticTree tree = new DiagnosticTree();

        reasons.forEach(reason -> {
            final HowIGotHere howIGotHere = reason.getHowIGotHere();
            final IdFor<? extends Location<?>> locationId = howIGotHere.getApproxLocation();
            if (locationId.isValid()) {
                final Node node = tree.addOrUpdateNode(locationId);

                if (howIGotHere.hasTowardsId()) {
                    final IdFor<Station> towardsId = howIGotHere.getTowardsId();
                    tree.updateRelationshipsFor(node, towardsId, reason);
                } else {
                    node.addReason(reason);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping node " + howIGotHere.getEndNodeId() + " has no position");
                }
            }

        });

        return createFrom(tree);

    }

    private JourneyDiagnostics createFrom(final DiagnosticTree tree) {
        final AtomicInteger maxNodeReasons = new AtomicInteger(0);
        final AtomicInteger maxEdgeReasons = new AtomicInteger(0);
        final List<StationDiagnosticsDTO> dto = tree.visit(node -> createLocationDiagnosticsDTO(node, maxEdgeReasons, maxNodeReasons));
        return new JourneyDiagnostics(dto, maxNodeReasons.get(), maxEdgeReasons.get());
    }

    @NotNull
    private StationDiagnosticsDTO createLocationDiagnosticsDTO(final Node node, final AtomicInteger maxEdgeReasons, final AtomicInteger maxNodeReasons) {

        final int currentMaxReasonsForEdges = getMaxNumReasons(node.edges.values());
        if (currentMaxReasonsForEdges > maxEdgeReasons.get()) {
            maxEdgeReasons.set(currentMaxReasonsForEdges);
        }
        final Set<HeuristicsReason> heuristicsReasons = node.reasonCodes;
        if (heuristicsReasons.size() > maxNodeReasons.get()) {
            maxNodeReasons.set(heuristicsReasons.size());
        }

        final Location<?> location = locationRepository.getLocation(node.locationId);
        final LocationRefWithPosition stationDto = new LocationRefWithPosition(location);
        final List<StationDiagnosticsLinkDTO> links = node.visitEdges(this::createStationDiagLinkDTO);

        return new StationDiagnosticsDTO(stationDto, convertReasons(heuristicsReasons), links, getCodes(heuristicsReasons));
    }

    private StationDiagnosticsLinkDTO createStationDiagLinkDTO(final Edge edge) {
        final Location<?> location = locationRepository.getLocation(edge.end.locationId);
        final LocationRefWithPosition towardsDTO = new LocationRefWithPosition(location);
        return new StationDiagnosticsLinkDTO(towardsDTO, convertReasons(edge.reasonCodes), getCodes(edge.reasonCodes));
    }

    private EnumSet<ReasonCode> getCodes(Set<HeuristicsReason> reasonCodes) {
        return reasonCodes.stream().map(HeuristicsReason::getReasonCode).collect(Collectors.toCollection(() -> EnumSet.noneOf(ReasonCode.class)));
    }

    private int getMaxNumReasons(final Collection<Edge> edges) {
        return edges.stream().mapToInt(item -> item.reasonCodes.size()).max().orElse(0);
    }

    private List<DiagnosticReasonDTO> convertReasons(final Set<HeuristicsReason> reasonCodes) {
        final Set<HeuristicsReason> consolidated = consolidateCodes(reasonCodes);
        return consolidated.stream().
                filter(reason -> isInteresting(reason.getReasonCode())).
                sorted(Comparator.comparingInt(a -> a.getReasonCode().ordinal())).
                map(CreateJourneyDiagnostics::createDiagnosticReasonDTO).distinct().collect(Collectors.toList());
    }

    // to keep things easy to understand and
    private Set<HeuristicsReason> consolidateCodes(final Set<HeuristicsReason> heuristicsReasons) {
        final Map<ReasonCode, Set<HeuristicsReason>> map = new HashMap<>();

        heuristicsReasons.forEach(reason -> {
            final ReasonCode reasonCode = reason.getReasonCode();
            if (!map.containsKey(reasonCode)) {
                map.put(reasonCode, new HashSet<>());
            }
            map.get(reasonCode).add(reason);
        });

        return map.entrySet().stream().
                flatMap(entry -> expand(entry.getKey(), entry.getValue().stream())).
                collect(Collectors.toSet());
    }

    private Stream<HeuristicsReason> expand(final ReasonCode code, final Stream<HeuristicsReason> reasons) {
        if (code==ReasonCode.NotAtHour || code==ReasonCode.ServiceNotRunningAtTime || code==ReasonCode.HourOk) {
            return consolidateMultipleByFirstAttribute(reasons);
        } else if (code==ReasonCode.CachedNotAtHour) {
            Stream<HeuristicsReason> cachedReasons = reasons.
                    map(reason -> (CachedHeuristicReason)reason).
                    map(CachedHeuristicReason::getContained);
            return consolidateMultipleByFirstAttribute(cachedReasons).map(CachedHeuristicReason::new);
        } else if (code==ReasonCode.TimeOk || code==ReasonCode.DoesNotOperateOnTime) {
            return Stream.of(consolidateSingle(reasons.toList()));
        }
        else {
            return reasons;
        }
    }

    private HeuristicsReason consolidateSingle(final List<HeuristicsReason> reasons) {
        Optional<String> attributeValues = reasons.stream().filter(reason -> reason instanceof HeuristicReasonWithAttribute).
                map(reason -> (HeuristicReasonWithAttribute<?>) reason).
                map(HeuristicReasonWithAttribute::getAttribute).
                map(item -> " " + item).
                reduce((s1, s2) -> s1 + s2);

        HeuristicsReason example = reasons.get(0);
        return new HeuristicReasonWithAttribute<>(example.getReasonCode(), example.getHowIGotHere(), attributeValues.orElse(""), example.isValid());

    }

    private Stream<HeuristicsReason> consolidateMultipleByFirstAttribute(final Stream<HeuristicsReason> reasons) {
        final Map<Object, Set<HeuristicReasonWithAttributes<?, ?>>> collected = new HashMap<>();
        reasons.
                filter(reason -> reason instanceof HeuristicReasonWithAttributes).
                map(reason -> (HeuristicReasonWithAttributes<?, ?>) reason).
                forEach(reason -> {
                    Object attributeA = reason.getAttributeA();
                    if (!collected.containsKey(attributeA)) {
                        collected.put(attributeA, new HashSet<>());
                    }
                    collected.get(attributeA).add(reason);
                });

        return collected.entrySet().stream().map(entry -> createRolledUp(entry.getKey(), entry.getValue()));
    }

    private HeuristicsReason createRolledUp(final Object key, final Set<HeuristicReasonWithAttributes<?, ?>> reasonsToConsolidate) {
        final Optional<HeuristicReasonWithAttributes<?, ?>> findExample = reasonsToConsolidate.stream().filter(item -> item.getAttributeA().equals(key)).findFirst();
        if (findExample.isEmpty()) {
            throw new RuntimeException("Could not find key " + key.toString() + " in " + reasonsToConsolidate);
        }
        final HeuristicReasonWithAttributes<?, ?> example = findExample.get();
        final Optional<String> reducded = reasonsToConsolidate.stream().map(item -> " " + item.getAttributeB().toString()).reduce((s1, s2) -> s1 + s2);
        final String text = reducded.orElse("");
        return new HeuristicReasonWithAttributes<>(example.getReasonCode(), example.getHowIGotHere(), key, text, example.isValid());
    }

    private boolean isInteresting(ReasonCode reasonCode) {
        return switch (reasonCode) {
            case Continue, NumWalkingConnectionsOk, PreviousCacheMiss, NeighbourConnectionsOk,
                    StationOpen, TransportModeOk, ServiceDateOk -> false;
            default -> true;
        };
    }

    @NotNull
    private static DiagnosticReasonDTO createDiagnosticReasonDTO(final HeuristicsReason heuristicsReason) {
        if (heuristicsReason.getReasonCode()==ReasonCode.NotOnQueryDate) {
            return new DiagnosticReasonDTO(ReasonCode.NotOnQueryDate, "NotOnQueryDate", heuristicsReason.isValid(),
                    heuristicsReason.getHowIGotHere().getTraversalStateType());
        }
        if (heuristicsReason.getReasonCode()==ReasonCode.CachedNotOnQueryDate) {
            return new DiagnosticReasonDTO(ReasonCode.CachedNotOnQueryDate, "CachedNotOnQueryDate", heuristicsReason.isValid(),
                    heuristicsReason.getHowIGotHere().getTraversalStateType());
        }
        return new DiagnosticReasonDTO(heuristicsReason);
    }

    private class DiagnosticTree {
        private final Map<IdFor<? extends Location<?>>, Node> nodes;

        public DiagnosticTree() {
            nodes = new HashMap<>();
        }

        public Node addOrUpdateNode(IdFor<? extends Location<?>> stationId) {

            final Node node;
            if (!nodes.containsKey(stationId)) {
                node = new Node(stationId);
                nodes.put(stationId, node);
            } else {
                node = nodes.get(stationId);
            }

            return node;
        }

        public void updateRelationshipsFor(Node node, IdFor<Station> towardsId, HeuristicsReason reason) {
            final Node towardsNode = addOrUpdateNode(towardsId);
            node.addEdgeTowards(towardsId, towardsNode, reason);
        }

        public <T> List<T> visit(Function<Node,T> visitor) {
            return nodes.values().stream().map(visitor).toList();
        }
    }

    private class Node {
        private final IdFor<? extends Location<?>> locationId;
        private final Map<IdFor<? extends Location<?>>, Edge> edges;
        private final Set<HeuristicsReason> reasonCodes;

        private Node(final IdFor<? extends Location<?>> locationId) {
            this.locationId = locationId;
            edges = new HashMap<>();
            reasonCodes = new HashSet<>();
        }

        public void addEdgeTowards(IdFor<Station> towardsId, Node towardsNode, HeuristicsReason heuristicsReason) {
            final Edge edge;
            if (edges.containsKey(towardsId)) {
                edge = edges.get(towardsId);
            } else {
                edge = new Edge(towardsNode);
                edges.put(towardsId, edge);
            }
            edge.addCode(heuristicsReason);
        }

        public void addReason(final HeuristicsReason heuristicsReason) {
            reasonCodes.add(heuristicsReason);
        }

        public <T> List<T> visitEdges(Function<Edge, T> function) {
            return edges.values().stream().map(function).toList();
        }

    }

    private class Edge {
        private final Node end;
        private final Set<HeuristicsReason> reasonCodes;

        private Edge(Node end) {
            this.end = end;
            reasonCodes = new HashSet<>();
        }

        public void addCode(HeuristicsReason heuristicsReason) {
            reasonCodes.add(heuristicsReason);
        }
    }
}
