package com.tramchester.graph.core.neo4j;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.RelationshipType;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@LazySingleton
public class RelationshipTypeFactory {
    private final Map<TransportRelationshipTypes, Container> map;
    private TransportRelationshipTypes relationshipType;

    public RelationshipTypeFactory() {
        map = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            map.put(relationshipType, new Container(relationshipType));
        }
    }

    public RelationshipType get(final TransportRelationshipTypes transportRelationshipType) {
        return map.get(transportRelationshipType);
    }

    public RelationshipType[] get(final TransportRelationshipTypes[] transportRelationshipTypes) {
        final RelationshipType[] result = new RelationshipType[transportRelationshipTypes.length];
        for (int i = 0; i < transportRelationshipTypes.length; i++) {
            result[i] = get(transportRelationshipTypes[i]);
        }
        return result;
    }

    public static class Container implements RelationshipType {
        private final String name;

        public Container(TransportRelationshipTypes transportRelationshipType) {
            this.name = transportRelationshipType.name();
        }

        @Override
        public String name() {
            return name;
        }
    }

}
