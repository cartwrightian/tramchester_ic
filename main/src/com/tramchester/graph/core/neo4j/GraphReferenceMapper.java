package com.tramchester.graph.core.neo4j;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.reference.GraphLabel;
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

    public static EnumSet<GraphLabel> from(final Iterable<Label> iter) {
        // results from perf test, seconds

        // 1.221
        final EnumSet<GraphLabel> result = EnumSet.noneOf(GraphLabel.class);
        for(final Label item : iter) {
            result.add(GraphLabel.valueOf(item.name()));
        }
        return result;

        // 1.284
//        final EnumSet<GraphLabel> result = EnumSet.noneOf(GraphLabel.class);
//        iter.forEach(item -> result.add(GraphLabel.valueOf(item.name())));
//        return result;

        // 1.688
        // return Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toCollection(() -> EnumSet.noneOf(GraphLabel.class)));

        // 3.849
        //  final Set<GraphLabel> set = Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toSet());
        //  return EnumSet.copyOf(set);
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
