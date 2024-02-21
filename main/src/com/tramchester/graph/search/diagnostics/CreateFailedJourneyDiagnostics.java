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
import com.tramchester.repository.StationRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@LazySingleton
public class CreateFailedJourneyDiagnostics {
    private static final Logger logger = LoggerFactory.getLogger(CreateFailedJourneyDiagnostics.class);

    private final StationRepository stationRepository;


    @Inject
    public CreateFailedJourneyDiagnostics(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public JourneyDiagnostics recordFailedJourneys(final List<HeuristicsReason> reasons) {
        final DiagnosticTree tree = new DiagnosticTree();

        reasons.forEach(reason -> {
            final HowIGotHere howIGotHere = reason.getHowIGotHere();
            final IdFor<Station> stationId = howIGotHere.getApproxLocation();
            if (stationId.isValid()) {
                final Node node = tree.addOrUpdateNode(stationId);

                if (howIGotHere.hasTowardsId()) {
                    final IdFor<Station> towardsId = howIGotHere.getTowardsId();
                    tree.updateRelationshipsFor(node, towardsId, reason);
                } else {
                    node.addReason(reason);
                }

            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping node " + howIGotHere.getEndNodeId() + " as no position");
                }
            }

        });

        return createFrom(tree);

    }

    private JourneyDiagnostics createFrom(final DiagnosticTree tree) {
        final AtomicInteger maxNodeReasons = new AtomicInteger(0);
        final AtomicInteger maxEdgeReasons = new AtomicInteger(0);
        final List<StationDiagnosticsDTO> dto = tree.visit(node -> {
            final Station station = stationRepository.getStationById(node.stationId);
            final LocationRefWithPosition stationDto = new LocationRefWithPosition(station);
            final List<StationDiagnosticsLinkDTO> links = node.visitEdges(this::createLink);
            int currentMaxReasonsForEdges = getMaxNumReasons(node.edges);
            if (currentMaxReasonsForEdges > maxEdgeReasons.get()) {
                maxEdgeReasons.set(currentMaxReasonsForEdges);
            }
            final Set<HeuristicsReason> heuristicsReasons = node.reasonCodes;
            if (heuristicsReasons.size() > maxNodeReasons.get()) {
                maxNodeReasons.set(heuristicsReasons.size());
            }
            return new StationDiagnosticsDTO(stationDto, convertReasons(heuristicsReasons), links, getCodes(heuristicsReasons));
        });
        return new JourneyDiagnostics(dto, maxNodeReasons.get(), maxEdgeReasons.get());
    }

    private EnumSet<ReasonCode> getCodes(Set<HeuristicsReason> reasonCodes) {
        return reasonCodes.stream().map(HeuristicsReason::getReasonCode).collect(Collectors.toCollection(() -> EnumSet.noneOf(ReasonCode.class)));
    }

    private int getMaxNumReasons(Map<IdFor<Station>, Edge> edges) {
        return edges.values().stream().mapToInt(item -> item.reasonCodes.size()).max().orElse(0);
    }

    private List<DiagnosticReasonDTO> convertReasons(final Set<HeuristicsReason> reasonCodes) {
        return reasonCodes.stream().
                filter(reason -> !reason.isValid()).
                //filter(reason -> !reason.isCached()).
                sorted(Comparator.comparingInt(a -> a.getReasonCode().ordinal())).
                map(CreateFailedJourneyDiagnostics::createDiagnosticReasonDTO).distinct().collect(Collectors.toList());
    }

    @NotNull
    private static DiagnosticReasonDTO createDiagnosticReasonDTO(HeuristicsReason heuristicsReason) {
        if (heuristicsReason.getReasonCode()==ReasonCode.NotOnQueryDate) {
            return new DiagnosticReasonDTO(ReasonCode.NotOnQueryDate, "NotOnQueryDate", heuristicsReason.isValid());
        }
        return new DiagnosticReasonDTO(heuristicsReason);
    }

    private StationDiagnosticsLinkDTO createLink(final Edge edge) {
        Location<?> station = stationRepository.getStationById(edge.end.stationId);
        LocationRefWithPosition towardsDTO = new LocationRefWithPosition(station);
        return new StationDiagnosticsLinkDTO(towardsDTO, convertReasons(edge.reasonCodes), getCodes(edge.reasonCodes));
    }

    private class DiagnosticTree {
        private final Map<IdFor<Station>,Node> nodes;

        public DiagnosticTree() {
            nodes = new HashMap<>();
        }

        public Node addOrUpdateNode(IdFor<Station> stationId) {

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
        private final IdFor<Station> stationId;
        private final Map<IdFor<Station>, Edge> edges;
        private final Set<HeuristicsReason> reasonCodes;

        private Node(IdFor<Station> stationId) {
            this.stationId = stationId;
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
