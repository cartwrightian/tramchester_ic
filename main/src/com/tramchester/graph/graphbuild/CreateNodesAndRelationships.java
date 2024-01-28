package com.tramchester.graph.graphbuild;

import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
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

    protected void addGroupRelationshipTowardsParent(MutableGraphTransaction txn, MutableGraphNode stationNode, MutableGraphNode groupNode, Duration walkCost) {
        addRelationshipFor(txn, stationNode, groupNode, walkCost, GROUPED_TO_PARENT);
    }

    protected void addGroupRelationshipTowardsChild(MutableGraphTransaction txn, MutableGraphNode groupNode, MutableGraphNode stationNode, Duration walkCost) {
        addRelationshipFor(txn, groupNode, stationNode, walkCost, GROUPED_TO_CHILD);
    }

    private boolean addRelationshipFor(final MutableGraphTransaction txn, final MutableGraphNode fromNode, final MutableGraphNode toNode,
                                       final Duration walkCost, final TransportRelationshipTypes relationshipType) {

        final boolean alreadyPresent = fromNode.getRelationships(txn, Direction.OUTGOING, relationshipType).
                map(relationship -> relationship.getEndNodeId(txn)).
                anyMatch(endNodeId -> endNodeId.equals(toNode.getId()));

        if (alreadyPresent) {
            return false;
        }

        final MutableGraphRelationship relationship = createRelationship(txn, fromNode, toNode, relationshipType);
        relationship.setCost(walkCost);
        relationship.setMaxCost(walkCost);
        return true;
    }
}
