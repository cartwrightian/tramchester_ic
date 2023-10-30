package com.tramchester.graph.graphbuild;

import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphTransaction;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphProps.setProperty;
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
        GraphNode stationNode = createGraphNode(tx, labels);
        setProperty(stationNode, station);
        return stationNode;
    }

    @Deprecated
    protected Node createGraphNodeOld(GraphTransaction tx, GraphLabel label) {
        numberNodes++;
        return tx.createNode(label).getNode();
//        return graphDatabase.createNode(tx, label);
    }

    protected GraphNode createGraphNode(GraphTransaction tx, GraphLabel label) {
        numberNodes++;
        return tx.createNode(label);
        //return GraphNode.from(graphDatabase.createNode(tx, label));
    }

    public GraphNode createGraphNode(GraphTransaction tx, Set<GraphLabel> labels) {
        numberNodes++;
        return tx.createNode(labels);
        //return graphDatabase.createNode(tx, labels);
    }

    protected Relationship createRelationship(GraphNode start, GraphNode end, TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(end, relationshipType);
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    protected boolean addNeighbourRelationship(GraphNode fromNode, GraphNode toNode, Duration walkCost) {
        return addRelationshipFor(fromNode, toNode, walkCost, NEIGHBOUR);
    }

    protected void addGroupRelationshipTowardsParent(GraphNode fromNode, GraphNode toNode, Duration walkCost) {
        addRelationshipFor(fromNode, toNode, walkCost, GROUPED_TO_PARENT);
    }

    protected void addGroupRelationshipTowardsChild(GraphNode fromNode, GraphNode toNode, Duration walkCost) {
        addRelationshipFor(fromNode, toNode, walkCost, GROUPED_TO_CHILD);
    }

    private boolean addRelationshipFor(GraphNode fromNode, GraphNode toNode, Duration walkCost, TransportRelationshipTypes relationshipType) {
        Set<GraphNode> alreadyRelationship = new HashSet<>();
        fromNode.getRelationships(Direction.OUTGOING, relationshipType).
                forEach(relationship -> alreadyRelationship.add(GraphNode.fromEnd(relationship)));

        if (!alreadyRelationship.contains(toNode)) {
            Relationship relationship = createRelationship(fromNode, toNode, relationshipType);
            GraphProps.setCostProp(relationship, walkCost);
            GraphProps.setMaxCostProp(relationship, walkCost);
            return true;
        }
        return false;
    }
}
