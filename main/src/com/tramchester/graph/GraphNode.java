package com.tramchester.graph;

import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.*;

import java.util.Map;
import java.util.Objects;

import static com.tramchester.graph.GraphPropertyKey.HOUR;
import static com.tramchester.graph.TransportRelationshipTypes.LINKED;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class GraphNode {
    private final Node neo4jNode;
    private final long id;

    public static GraphNode from(Node neo4jNode) {
        // preserve existing i/f for now
        if (neo4jNode==null) {
            return null;
        }
        return new GraphNode(neo4jNode);
    }

    private GraphNode(Node neo4jNode) {
        this.neo4jNode = neo4jNode;

        // todo remove/replace with get element Id
        this.id = neo4jNode.getId();
    }

    public static GraphNode fromEnd(Relationship relationship) {
        return from(relationship.getEndNode());
    }

    public static GraphNode fromStart(Relationship relationship) {
        return from(relationship.getStartNode());
    }

    @Deprecated
    public static GraphNode fromTransaction(Transaction txn, Long nodeId) {
        return from(txn.getNodeById(nodeId));
    }



    @Deprecated
    public Long getId() {
        return id;
    }

    public Node getNode() {
        return neo4jNode;
    }

    public Relationship createRelationshipTo(GraphNode otherNode, TransportRelationshipTypes direction) {
        return neo4jNode.createRelationshipTo(otherNode.neo4jNode, direction);
    }

    public void delete() {
        neo4jNode.delete();
    }

    public ResourceIterable<Relationship> getRelationships(Direction direction, TransportRelationshipTypes relationshipType) {
        return neo4jNode.getRelationships(direction, relationshipType);
    }

    public ResourceIterable<Relationship> getRelationships(Direction direction, TransportRelationshipTypes[] transportRelationshipTypes) {
        return neo4jNode.getRelationships(direction, transportRelationshipTypes);
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "neo4jNode=" + neo4jNode +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return id == graphNode.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean hasRelationship(Direction direction, TransportRelationshipTypes transportRelationshipTypes) {
        return neo4jNode.hasRelationship(direction, transportRelationshipTypes);
    }

    public void addLabel(Label label) {
        neo4jNode.addLabel(label);
    }

    public void setHourProp(int hour) {
        neo4jNode.setProperty(HOUR.getText(), hour);
    }

    public Map<String, Object> getAllProperties() {
        return neo4jNode.getAllProperties();
    }

    public boolean hasLabel(GraphLabel graphLabel) {
        return neo4jNode.hasLabel(graphLabel);
    }

    public Object getProperty(String key) {
        return neo4jNode.getProperty(key);
    }

    public Relationship getSingleRelationship(TransportRelationshipTypes transportRelationshipTypes, Direction direction) {
        return neo4jNode.getSingleRelationship(transportRelationshipTypes,direction);
    }
}
