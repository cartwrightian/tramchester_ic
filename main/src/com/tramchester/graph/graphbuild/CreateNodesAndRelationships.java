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
import java.util.HashSet;
import java.util.Set;

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

    protected GraphNode createStationNode(GraphTransaction tx, Station station) {

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
    protected Node createGraphNodeOld(GraphTransaction tx, GraphLabel label) {
        numberNodes++;
        return tx.createNode(label).getNode();
    }

    protected MutableGraphNode createGraphNode(GraphTransaction tx, GraphLabel label) {
        numberNodes++;
        return tx.createNode(label);
    }

    public MutableGraphNode createGraphNode(GraphTransaction tx, Set<GraphLabel> labels) {
        numberNodes++;
        return tx.createNode(labels);
        //return graphDatabase.createNode(tx, labels);
    }

    protected MutableGraphRelationship createRelationship(GraphTransaction txn, MutableGraphNode start, MutableGraphNode end, TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(txn, end, relationshipType);
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    protected boolean addNeighbourRelationship(GraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode, Duration walkCost) {
        return addRelationshipFor(txn, fromNode, toNode, walkCost, NEIGHBOUR);
    }

    protected void addGroupRelationshipTowardsParent(GraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode, Duration walkCost) {
        addRelationshipFor(txn, fromNode, toNode, walkCost, GROUPED_TO_PARENT);
    }

    protected void addGroupRelationshipTowardsChild(GraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode, Duration walkCost) {
        addRelationshipFor(txn, fromNode, toNode, walkCost, GROUPED_TO_CHILD);
    }

    private boolean addRelationshipFor(GraphTransaction txn, MutableGraphNode fromNode, MutableGraphNode toNode, Duration walkCost, TransportRelationshipTypes relationshipType) {
        Set<GraphNode> alreadyRelationship = new HashSet<>();
        fromNode.getRelationships(txn, Direction.OUTGOING, relationshipType).
                forEach(relationship -> alreadyRelationship.add(relationship.getEndNode(txn)));

        if (!alreadyRelationship.contains(toNode)) {
            MutableGraphRelationship relationship = createRelationship(txn, fromNode, toNode, relationshipType);
            relationship.setCost(walkCost);
            relationship.setMaxCost(walkCost);
            return true;
        }
        return false;
    }
}
