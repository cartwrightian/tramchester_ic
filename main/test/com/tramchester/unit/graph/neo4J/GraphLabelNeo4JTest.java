package com.tramchester.unit.graph.neo4J;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.core.neo4j.GraphReferenceMapper;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.testSupport.testTags.Neo4JTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.reference.GraphLabel.HOUR_23;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Neo4JTest
public class GraphLabelNeo4JTest {

    GraphReferenceMapper mapper;

    @BeforeEach
    void onceBeforeEachTest() {
        mapper = new GraphReferenceMapper();
        mapper.start();
    }

    @Test
    void shouldMapToLabels() {
        EnumSet<GraphLabel> all = EnumSet.allOf(GraphLabel.class);

        all.forEach(graphLabel -> {
            Label label = mapper.get(graphLabel);
            assertNotNull(label,"failed for " + graphLabel.name());
            assertEquals(graphLabel.name(), label.name(), "failed for " + graphLabel.name());
        });
    }

    @Test
    void shouldGetFromIterable() {
        ImmutableEnumSet<GraphLabel> graphLabels = ImmutableEnumSet.range(GraphLabel.HOUR_0, HOUR_23);

        Set<Label> labels = graphLabels.stream().map(mapper::get).collect(Collectors.toSet());

        Iterable<Label> iterable = new Iterable<>() {
            @NotNull
            @Override
            public Iterator<Label> iterator() {
                return labels.iterator();
            }
        };

        ImmutableEnumSet<GraphLabel> result = GraphReferenceMapper.from(iterable);
        assertEquals(graphLabels, result);
    }

    @Disabled("performance testing")
    @Test
    void performanceTestForFromIterable() {
        final Set<Label> labels = EnumSet.range(GraphLabel.HOUR_0, HOUR_23).stream().
                map(graphLabel -> mapper.get(graphLabel)).
                collect(Collectors.toSet());

        Iterable<Label> iterable = new Iterable<>() {
            @NotNull
            @Override
            public Iterator<Label> iterator() {
                return labels.iterator();
            }
        };

        for (int i = 0; i < 10000000; i++) {
            GraphReferenceMapper.from(iterable);
        }
    }

//    @Disabled("performance testing")
//    @Test
//    void performanceTestForGetHourFrom() {
//        final EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.TRAM, GraphLabel.TRAIN, GraphLabel.HOUR, GraphLabel.HOUR_4);
//
//        for (int i = 0; i < 1000000000; i++) {
//            GraphLabel.getHourFrom(labels);
//        }
//    }

}
