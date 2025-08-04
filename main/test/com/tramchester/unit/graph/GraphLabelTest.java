package com.tramchester.unit.graph;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.neo4j.GraphReferenceMapper;
import com.tramchester.graph.reference.GraphLabel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
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

    GraphReferenceMapper mapper;

    @BeforeEach
    void onceBeforeEachTest() {
        mapper = new GraphReferenceMapper();
        mapper.start();
    }


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
    void shouldGetHourFromLabels() {
        EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.TRAM, GraphLabel.TRAIN, GraphLabel.HOUR, GraphLabel.HOUR_4);
        int result = GraphLabel.getHourFrom(labels);
        assertEquals(4, result);
    }

    @Test
    void shouldGetGraphLabelsFromModes() {
        EnumSet<TransportMode> modes = getTransportModes();

        EnumSet<GraphLabel> results = GraphLabel.forModes(modes);
        int size = modes.size() - 2; // Ferry&Ship->Ferry, Rail&RailReplacementBus->Train
        assertEquals(size, results.size());
    }

    @Test
    void shouldGetSetOfLabelsForModes() {
        EnumSet<TransportMode> modes = getTransportModes();

        EnumSet<GraphLabel> results = GraphLabel.forModes(modes);
        assertEquals(GraphLabel.TransportModes, results);
    }

    private static @NotNull EnumSet<TransportMode> getTransportModes() {
        EnumSet<TransportMode> modes = EnumSet.allOf(TransportMode.class);
        modes.remove(TransportMode.Connect);
        modes.remove(TransportMode.NotSet);
        modes.remove(TransportMode.Unknown);
        modes.remove(TransportMode.Walk); // TODO inconsistency on use of QUERY_NODE for walks, needs sorting out
        return modes;
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
        EnumSet<GraphLabel> graphLabels = EnumSet.range(GraphLabel.HOUR_0, GraphLabel.HOUR_23);

        Set<Label> labels = graphLabels.stream().map(mapper::get).collect(Collectors.toSet());

        Iterable<Label> iterable = new Iterable<>() {
            @NotNull
            @Override
            public Iterator<Label> iterator() {
                return labels.iterator();
            }
        };

        EnumSet<GraphLabel> result = GraphReferenceMapper.from(iterable);
        assertEquals(graphLabels, result);
    }

    @Disabled("performance testing")
    @Test
    void performanceTestForFromIterable() {
        final Set<Label> labels = EnumSet.range(GraphLabel.HOUR_0, GraphLabel.HOUR_23).stream().
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

    @Disabled("performance testing")
    @Test
    void performanceTestForGetHourFrom() {
        final EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.TRAM, GraphLabel.TRAIN, GraphLabel.HOUR, GraphLabel.HOUR_4);

        for (int i = 0; i < 1000000000; i++) {
            GraphLabel.getHourFrom(labels);
        }
    }

}
