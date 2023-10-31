package com.tramchester.graph.search.diagnostics;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.*;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.RelationshipType;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class ReasonsToGraphViz {

    private final NaptanRepository naptanRespository;
    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeContentsRepository;

    private static final boolean includeAll = false;

    @Inject
    public ReasonsToGraphViz(NaptanRepository naptanRespository, StationRepository stationRepository,
                             NodeContentsRepository nodeContentsRepository) {
        this.naptanRespository = naptanRespository;
        this.stationRepository = stationRepository;
        this.nodeContentsRepository = nodeContentsRepository;
    }

    public void appendTo(StringBuilder builder, List<HeuristicsReason> reasons, GraphTransaction txn) {
        DiagramState diagramState = new DiagramState();
        reasons.forEach(reason -> add(reason, txn, builder, diagramState));
        diagramState.clear();
    }

    private void add(HeuristicsReason reason, GraphTransaction transaction, StringBuilder builder, DiagramState diagramState) {
        HowIGotHere howIGotHere = reason.getHowIGotHere();

        GraphNodeId endNodeId = howIGotHere.getEndNodeId();
        String reasonId = reason.getReasonCode().name() + endNodeId;
        String stateName = howIGotHere.getTraversalStateName();
        GraphNode currentNode = transaction.getNodeById(endNodeId);

        addNodeToDiagram(currentNode, builder, diagramState, stateName);

        if (includeAll || !reason.isValid()) {
            if (!diagramState.reasonIds.contains(reasonId)) {
                diagramState.reasonIds.add(reasonId);
                String shape = reason.isValid() ? "oval" : "octagon";
                builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", reasonId, reason.textForGraph(), shape));
            }

            Pair<GraphNodeId, String> reasonLink = Pair.of(endNodeId, reasonId);
            if (!diagramState.reasonRelationships.contains(reasonLink)) {
                diagramState.reasonRelationships.add(reasonLink);
                builder.append(format("\"%s\"->\"%s\"", endNodeId, reasonId));
            }
        }

        if (!howIGotHere.atStart()) {
            GraphRelationship relationship = transaction.getRelationshipById(howIGotHere.getRelationshipId());
            GraphNode fromNode = GraphNode.fromStart(relationship); // relationship.getStartNode();
            addNodeToDiagram(fromNode, builder, diagramState, stateName);

            GraphNodeId fromNodeId = fromNode.getId();
            Pair<GraphNodeId,GraphNodeId> link = Pair.of(fromNodeId, endNodeId);
            if (!diagramState.relationships.contains(link)) {
                diagramState.relationships.add(link);
                RelationshipType relationshipType = relationship.getType();
                builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", fromNodeId, endNodeId, relationshipType.name()));
            }
        }
    }

    private void addNodeToDiagram(GraphNode node, StringBuilder builder, DiagramState diagramState, String stateName) {
        GraphNodeId nodeId = node.getId();
        if (!diagramState.nodes.contains(nodeId)) {
            diagramState.nodes.add(nodeId);
            StringBuilder nodeLabel = new StringBuilder();
            nodeContentsRepository.getLabels(node).forEach(label -> nodeLabel.append(label.name()).append(" "));
            String ids = getIdsFor(node);
            nodeLabel.append("\n").append(ids).append("\n").append(stateName);
            builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", nodeId, nodeLabel, "hexagon"));
        }
    }

    private String getIdsFor(GraphNode node) {
        StringBuilder ids = new StringBuilder();
        EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(node);

        if (labels.contains(GraphLabel.GROUPED)) {
            IdFor<NaptanArea> areaId = GraphProps.getAreaIdFromGrouped(node);
            NaptanArea area = naptanRespository.getAreaFor(areaId);
            ids.append(System.lineSeparator()).append(area.getName());
            return ids.toString();
        }

        if (labels.contains(GraphLabel.STATION)) {
            IdFor<Station> stationIdFrom = GraphProps.getStationIdFrom(node);
            Station station = stationRepository.getStationById(stationIdFrom);
            ids.append(System.lineSeparator()).append(station.getName());
        }

        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            IdFor<Station> stationIdFrom = GraphProps.getStationIdFrom(node);
            Station station = stationRepository.getStationById(stationIdFrom);
            ids.append(System.lineSeparator()).append(station.getName());
            String value = GraphProps.getRouteIdFrom(node).toString();
            ids.append(System.lineSeparator());
            ids.append(value);
        }

        if (labels.contains(GraphLabel.INTERCHANGE)) {
            ids.append(System.lineSeparator());
            ids.append("INTERCHANGE");
        }

        if (labels.contains(GraphLabel.MINUTE)) {
            TramTime time = GraphProps.getTime(node);
            ids.append(time.toString());
            String value = GraphProps.getTripId(node).toString();
            ids.append(System.lineSeparator());
            ids.append(value);
        }

        return ids.toString();
    }


    private static class DiagramState {
        private final Set<GraphNodeId> nodes;
        private final Set<String> reasonIds;
        private final Set<Pair<GraphNodeId,GraphNodeId>> relationships;
        private final Set<Pair<GraphNodeId, String>> reasonRelationships;

        private DiagramState() {
            nodes = new HashSet<>();
            reasonIds = new HashSet<>();
            relationships = new HashSet<>();
            reasonRelationships = new HashSet<>();
        }

        public void clear() {
            nodes.clear();
            reasonIds.clear();
            relationships.clear();
            reasonRelationships.clear();
        }
    }
}
