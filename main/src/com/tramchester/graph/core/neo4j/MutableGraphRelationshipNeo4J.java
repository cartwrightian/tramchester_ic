package com.tramchester.graph.core.neo4j;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.caches.SharedRelationshipCache;
import com.tramchester.graph.core.*;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.Objects;

public class MutableGraphRelationshipNeo4J extends GraphRelationshipProperties<GraphPropsNeo4J> {

    private final Relationship relationship;
    private final GraphRelationshipId id;
    private final SharedRelationshipCache.InvalidatesCache invalidatesCacheFor;
    private final GraphReferenceMapper relationshipTypeFactory;

    private GraphNode endNode;

    MutableGraphRelationshipNeo4J(final Relationship relationship, final GraphRelationshipId id, GraphReferenceMapper relationshipTypeFactory,
                                  SharedRelationshipCache.InvalidatesCache invalidatesCacheFor) {
        super(GraphPropsNeo4J.wrap(relationship));
        this.relationship = relationship;
        this.id = id;
        this.relationshipTypeFactory = relationshipTypeFactory;
        this.invalidatesCacheFor = invalidatesCacheFor;
    }

    @Override
    public GraphRelationshipId getId() {
        return id;
    }

    @Override
    public void delete(MutableGraphTransaction txn) {
        invalidateCache();
        relationship.delete();
    }

    @Override
    public void invalidateCache() {
        invalidatesCacheFor.remove();
    }

    @Override
    public boolean isType(TransportRelationshipTypes transportRelationshipType) {
        final RelationshipType theType = relationshipTypeFactory.get(transportRelationshipType);
        return relationship.isType(theType);
    }

    @Override
    public TransportRelationshipTypes getType() {
        return TransportRelationshipTypes.from(relationship.getType().name());
    }

    public GraphNode getStartNode(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.getStartNode(relationship);
    }

    @Override
    public GraphNode getEndNode(final GraphTransaction txn) {
        if (endNode==null) {
            final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
            endNode = txnNeo4J.getEndNode(relationship);
        }
        return endNode;
    }

    @Override
    public GraphNodeId getStartNodeId(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.getStartNodeId(relationship);
    }

    @Override
    public GraphNodeId getEndNodeId(final GraphTransaction txn) {
        if (endNode==null) {
            final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
            return txnNeo4J.getEndNodeId(relationship);
        } else {
            return endNode.getId();
        }
    }

//    @Override
//    public boolean hasProperty(final GraphPropertyKey propertyKey) {
//        return relationship.hasProperty(propertyKey.getText());
//    }

    @Override
    public IdFor<Station> getEndStationId(GraphTransaction txn) {
        return getIdFor(Station.class, GraphPropsNeo4J.wrap(relationship.getEndNode()));
    }

    @Override
    public IdFor<Station> getStartStationId(GraphTransaction txn) {
        return getIdFor(Station.class, GraphPropsNeo4J.wrap(relationship.getStartNode()));
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId(GraphTransaction txn) {
        return getIdFor(StationLocalityGroup.class, GraphPropsNeo4J.wrap(relationship.getEndNode()));
    }

    Relationship getRelationship() {
        return relationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableGraphRelationshipNeo4J that = (MutableGraphRelationshipNeo4J) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final TransportRelationshipTypes relationshipType = getType();
        final String key = getExtraDiagnostics(relationshipType);
        return "MutableGraphRelationship{" +
                "type=" + relationshipType +
                key +
                " id=" + id +
                "} ";
    }

    @NotNull
    private String getExtraDiagnostics(TransportRelationshipTypes relationshipType) {
        final String extra;
        // TODO Include more types here, aids with debug etc
        if (relationshipType==TransportRelationshipTypes.TO_SERVICE) {
            extra = " serviceId=" + getServiceId()
                    + " routeId=" + getRouteId();
        }
        else if (relationshipType==TransportRelationshipTypes.INTERCHANGE_DEPART || relationshipType==TransportRelationshipTypes.DEPART) {
            extra = " routeStationId=" + getRouteStationId();
        } else {
            extra = "";
        }
        return extra;
    }

    @Override
    public boolean isNode() {
        return false;
    }

    @Override
    public boolean isRelationship() {
        return true;
    }
}
