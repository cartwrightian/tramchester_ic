package com.tramchester.graph.graphbuild;

import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;

public class CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(CreateNodesAndRelationships.class);

    protected final GraphDatabase graphDatabase;

    private int numberNodes;
    private int numberRelationships;

    public CreateNodesAndRelationships(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
        numberNodes = 0;
        numberRelationships = 0;
    }

    protected GraphNode createStationNode(MutableGraphTransaction tx, Station station) {

        Set<GraphLabel> labels = GraphLabel.forMode(station.getTransportModes());
        labels.add(GraphLabel.STATION);
        if (station.hasPlatforms()) {
            labels.add(GraphLabel.HAS_PLATFORMS);
        }
        logger.debug(format("Creating station node: %s with labels: %s ", station, labels));
        MutableGraphNode stationNode = createGraphNode(tx, labels);
        stationNode.set(station);
        return stationNode;
    }

    @Deprecated
    protected Node createGraphNodeOld(MutableGraphTransaction tx, GraphLabel label) {
        numberNodes++;
        return tx.createNode(label).getNode();
    }

    protected MutableGraphNode createGraphNode(MutableGraphTransaction tx, GraphLabel label) {
        numberNodes++;
        return tx.createNode(label);
    }

    public MutableGraphNode createGraphNode(MutableGraphTransaction tx, Set<GraphLabel> labels) {
        numberNodes++;
        return tx.createNode(labels);
        //return graphDatabase.createNode(tx, labels);
    }

    protected MutableGraphRelationship createRelationship(MutableGraphTransaction txn, MutableGraphNode start, MutableGraphNode end, TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(txn, end, relationshipType);
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    protected boolean addNeighbourRelationship(MutableGraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode, Duration walkCost) {
        return addRelationshipFor(txn, fromNode, toNode, walkCost, NEIGHBOUR);
    }

    protected void addGroupRelationshipTowardsParent(MutableGraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode, Duration walkCost) {
        addRelationshipFor(txn, fromNode, toNode, walkCost, GROUPED_TO_PARENT);
    }

    protected void addGroupRelationshipTowardsChild(MutableGraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode, Duration walkCost) {
        addRelationshipFor(txn, fromNode, toNode, walkCost, GROUPED_TO_CHILD);
    }

    private boolean addRelationshipFor(MutableGraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode,
                                       Duration walkCost, TransportRelationshipTypes relationshipType) {

        Set<GraphNodeId> alreadyPresent = fromNode.getRelationships(txn, Direction.OUTGOING, relationshipType).
                map(relationship -> relationship.getEndNodeId(txn)).
                collect(Collectors.toSet());

        if (!alreadyPresent.contains(toNode.getId())) {
            MutableGraphRelationship relationship = createRelationship(txn, fromNode, toNode, relationshipType);
            relationship.setCost(walkCost);
            relationship.setMaxCost(walkCost);
            return true;
        }
        return false;
    }
}
