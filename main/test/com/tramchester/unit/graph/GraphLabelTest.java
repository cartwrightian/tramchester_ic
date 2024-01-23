package com.tramchester.unit.graph;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GraphLabelTest {

    @Test
    void shouldHaveLabelForEachValidMode() {
        Set<TransportMode> modes = Arrays.stream(TransportMode.values()).
                filter(mode -> mode != TransportMode.Connect).
                filter(mode -> mode != TransportMode.NotSet).
                filter(mode -> mode != TransportMode.Unknown).
                collect(Collectors.toSet());
        for(TransportMode mode : modes) {
            assertNotNull(GraphLabel.forMode(mode));
        }
    }

    @Test
    void shouldGetLabelForHour() {
        assertEquals(GraphLabel.HOUR_15, GraphLabel.getHourLabel(15));
    }

    @Test
    void shouldGetHourFromLables() {
        EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.TRAM, GraphLabel.TRAIN, GraphLabel.HOUR, GraphLabel.HOUR_4);
        int result = GraphLabel.getHourFrom(labels);
        assertEquals(4, result);
    }

    @Test
    void shouldGetFromIterable() {
        EnumSet<GraphLabel> graphLabels = EnumSet.range(GraphLabel.HOUR_0, GraphLabel.HOUR_23);

        Set<Label> labels = graphLabels.stream().map(graphLabel -> (Label)graphLabel).collect(Collectors.toSet());

        Iterable<Label> iterable = new Iterable<>() {
            @NotNull
            @Override
            public Iterator<Label> iterator() {
                return labels.iterator();
            }
        };

        EnumSet<GraphLabel> result = GraphLabel.from(iterable);
        assertEquals(graphLabels, result);
    }

    @Disabled("performance testing")
    @Test
    void performanceTestForFromIterable() {
        final Set<Label> labels = EnumSet.range(GraphLabel.HOUR_0, GraphLabel.HOUR_23).stream().
                map(graphLabel -> (Label)graphLabel).
                collect(Collectors.toSet());

        Iterable<Label> iterable = new Iterable<>() {
            @NotNull
            @Override
            public Iterator<Label> iterator() {
                return labels.iterator();
            }
        };

        for (int i = 0; i < 10000000; i++) {
            GraphLabel.from(iterable);
        }
    }

    @Disabled("performance testing")
    @Test
    void performanceTestForGetHourFrom() {
        final EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.TRAM, GraphLabel.TRAIN, GraphLabel.HOUR, GraphLabel.HOUR_4);

        for (int i = 0; i < 1000000000; i++) {
            GraphLabel.getHourFrom(labels);
        }
    }

}
