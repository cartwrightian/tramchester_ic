package com.tramchester.graph.graphbuild;

import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.MutableGraphNodeNeo4J;
import com.tramchester.graph.facade.neo4j.MutableGraphRelationship;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;
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

    protected GraphNode createStationNode(final MutableGraphTransaction tx, final Station station) {

        final EnumSet<GraphLabel> labels = GraphLabel.forModes(station.getTransportModes());
        labels.add(GraphLabel.STATION);
        if (station.hasPlatforms()) {
            labels.add(GraphLabel.HAS_PLATFORMS);
        }
        logger.debug(format("Creating station node: %s with labels: %s ", station, labels));
        final MutableGraphNodeNeo4J stationNode = createGraphNode(tx, labels);
        stationNode.set(station);
        return stationNode;
    }

    protected MutableGraphNodeNeo4J createGraphNode(final MutableGraphTransaction tx, final GraphLabel label) {
        numberNodes++;
        return tx.createNode(label);
    }

    public MutableGraphNodeNeo4J createGraphNode(final MutableGraphTransaction tx, final EnumSet<GraphLabel> labels) {
        numberNodes++;
        return tx.createNode(labels);
    }

    protected MutableGraphRelationship createRelationship(final MutableGraphTransaction txn, final MutableGraphNodeNeo4J start,
                                                          final MutableGraphNodeNeo4J end, final TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(txn, end, relationshipType);
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    protected void addNeighbourRelationship(final MutableGraphTransactionNeo4J txn, final MutableGraphNodeNeo4J fromNode,
                                            final MutableGraphNodeNeo4J toNode, final Duration walkCost) {
        addRelationshipFor(txn, fromNode, toNode, walkCost, NEIGHBOUR);
    }

    protected void addContainedRelationshipTowardsGroup(final MutableGraphTransactionNeo4J txn, final MutableGraphNodeNeo4J stationNode,
                                                        final MutableGraphNodeNeo4J groupNode, final Duration walkCost) {
        addRelationshipFor(txn, stationNode, groupNode, walkCost, GROUPED_TO_PARENT);
    }

    protected void addGroupRelationshipTowardsContained(final MutableGraphTransactionNeo4J txn, final MutableGraphNodeNeo4J groupNode,
                                                        final MutableGraphNodeNeo4J stationNode, final Duration walkCost) {
        addRelationshipFor(txn, groupNode, stationNode, walkCost, GROUPED_TO_CHILD);
    }

    protected void addRelationshipsBetweenGroupAndParentGroup(final MutableGraphTransactionNeo4J txn, final MutableGraphNodeNeo4J childGroupNode,
                                                              final MutableGraphNodeNeo4J parentGroupNode, final Duration walkCost) {
        addRelationshipFor(txn, parentGroupNode, childGroupNode, walkCost, GROUPED_TO_GROUPED);
        addRelationshipFor(txn, childGroupNode, parentGroupNode, walkCost, GROUPED_TO_GROUPED);
    }

    private boolean addRelationshipFor(final MutableGraphTransactionNeo4J txn, final MutableGraphNodeNeo4J fromNode, final MutableGraphNodeNeo4J toNode,
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
