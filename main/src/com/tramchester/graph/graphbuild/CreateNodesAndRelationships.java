package com.tramchester.graph.graphbuild;

import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;

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

    protected GraphNode createStationNode(final MutableGraphTransactionNeo4J tx, final Station station) {

        final EnumSet<GraphLabel> labels = GraphLabel.forModes(station.getTransportModes());
        labels.add(GraphLabel.STATION);
        if (station.hasPlatforms()) {
            labels.add(GraphLabel.HAS_PLATFORMS);
        }
        logger.debug(format("Creating station node: %s with labels: %s ", station, labels));
        final MutableGraphNode stationNode = createGraphNode(tx, labels);
        stationNode.set(station);
        return stationNode;
    }

    protected MutableGraphNode createGraphNode(final MutableGraphTransactionNeo4J tx, final GraphLabel label) {
        numberNodes++;
        return tx.createNode(label);
    }

    public MutableGraphNode createGraphNode(final MutableGraphTransactionNeo4J tx, final EnumSet<GraphLabel> labels) {
        numberNodes++;
        return tx.createNode(labels);
    }

    protected MutableGraphRelationship createRelationship(final MutableGraphTransactionNeo4J txn, final MutableGraphNode start,
                                                          final MutableGraphNode end, final TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(txn, end, relationshipType);
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    protected void addNeighbourRelationship(final MutableGraphTransactionNeo4J txn, final MutableGraphNode fromNode,
                                            final MutableGraphNode toNode, final Duration walkCost) {
        addRelationshipFor(txn, fromNode, toNode, walkCost, NEIGHBOUR);
    }

    protected void addContainedRelationshipTowardsGroup(final MutableGraphTransactionNeo4J txn, final MutableGraphNode stationNode,
                                                        final MutableGraphNode groupNode, final Duration walkCost) {
        addRelationshipFor(txn, stationNode, groupNode, walkCost, GROUPED_TO_PARENT);
    }

    protected void addGroupRelationshipTowardsContained(final MutableGraphTransactionNeo4J txn, final MutableGraphNode groupNode,
                                                        final MutableGraphNode stationNode, final Duration walkCost) {
        addRelationshipFor(txn, groupNode, stationNode, walkCost, GROUPED_TO_CHILD);
    }

    protected void addRelationshipsBetweenGroupAndParentGroup(final MutableGraphTransactionNeo4J txn, final MutableGraphNode childGroupNode,
                                                              final MutableGraphNode parentGroupNode, final Duration walkCost) {
        addRelationshipFor(txn, parentGroupNode, childGroupNode, walkCost, GROUPED_TO_GROUPED);
        addRelationshipFor(txn, childGroupNode, parentGroupNode, walkCost, GROUPED_TO_GROUPED);
    }

    private boolean addRelationshipFor(final MutableGraphTransactionNeo4J txn, final MutableGraphNode fromNode, final MutableGraphNode toNode,
                                       final Duration walkCost, final TransportRelationshipTypes relationshipType) {

        final boolean alreadyPresent = fromNode.getRelationships(txn, GraphDirection.Outgoing, relationshipType).
                map(relationship -> relationship.getEndNodeId(txn)).
                anyMatch(endNodeId -> endNodeId.equals(toNode.getId()));

        if (alreadyPresent) {
            return false;
        }

        final MutableGraphRelationship relationship = createRelationship(txn, fromNode, toNode, relationshipType);
        relationship.setCost(walkCost);
        return true;
    }
}
