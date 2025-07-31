package com.tramchester.graph.core.neo4j;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import javax.annotation.PostConstruct;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@LazySingleton
public class GraphReferenceMapper {
    private final Map<TransportRelationshipTypes, RelationshipContainer> relationshipMap;
    private final Map<GraphLabel, LabelContainer> labelMap;

    public GraphReferenceMapper() {
        relationshipMap = new HashMap<>();
        labelMap = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            relationshipMap.put(relationshipType, new RelationshipContainer(relationshipType));
        }
        for(GraphLabel label : GraphLabel.values()) {
            labelMap.put(label, new LabelContainer(label));
        }
    }

    public RelationshipType get(final TransportRelationshipTypes transportRelationshipType) {
        return relationshipMap.get(transportRelationshipType);
    }

    public RelationshipType[] get(final TransportRelationshipTypes[] transportRelationshipTypes) {
        final RelationshipType[] result = new RelationshipType[transportRelationshipTypes.length];
        for (int i = 0; i < transportRelationshipTypes.length; i++) {
            result[i] = get(transportRelationshipTypes[i]);
        }
        return result;
    }

    public Label get(final GraphLabel graphLabel) {
        if (!labelMap.containsKey(graphLabel)) {
            throw new RuntimeException("Missing for " + graphLabel);
        }
        return labelMap.get(graphLabel);
    }

    public Label[] get(final EnumSet<GraphLabel> labels) {
        final Label[] dest = new Label[labels.size()];
        int count = 0;
        for (final GraphLabel label : labels) {
            dest[count++] = labelMap.get(label);
        }
        return dest;
    }

    public static class RelationshipContainer implements RelationshipType {
        private final String name;

        public RelationshipContainer(TransportRelationshipTypes transportRelationshipType) {
            this.name = transportRelationshipType.name();
        }

        @Override
        public String name() {
            return name;
        }
    }

    private static class LabelContainer implements Label {
        private final String name;

        private LabelContainer(final GraphLabel graphLabel) {
            this.name = graphLabel.name();
        }

        @Override
        public String name() {
            return name;
        }
    }
}
