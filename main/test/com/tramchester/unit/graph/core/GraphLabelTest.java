package com.tramchester.unit.graph.core;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.reference.GraphLabel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.reference.GraphLabel.*;
import static org.junit.jupiter.api.Assertions.*;

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
    void shouldGetHourFromSetOfLabels() {
        ImmutableEnumSet<GraphLabel> setA = ImmutableEnumSet.of(HOUR, HOUR_9);
        assertEquals(9, GraphLabel.getHourFrom(setA));

        ImmutableEnumSet<GraphLabel> setB = ImmutableEnumSet.of(HOUR, HOUR_23);
        assertEquals(23, GraphLabel.getHourFrom(setB));
    }

    @Test
    void shouldRoundTripHours() {
        for (int hour = 0; hour <23; hour++) {
            GraphLabel label = GraphLabel.getHourLabel(hour);
            int result = getHourFrom(ImmutableEnumSet.of(HOUR, label));
            assertEquals(hour, result);
        }
    }

    @Test
    void shouldGetGraphLabelsFromModes() {
        ImmutableEnumSet<TransportMode> modes = getTransportModes();

        ImmutableEnumSet<GraphLabel> results = GraphLabel.forModes(modes);
        int size = modes.size() - 2; // Ferry&Ship->Ferry, Rail&RailReplacementBus->Train
        assertEquals(size, results.size());
    }

    @Test
    void shouldHaveSameSingleton() {
        assertSame(TRAM.singleton(), TRAM.singleton());
    }

    @Test
    void shouldGetSetOfLabelsForModes() {
        ImmutableEnumSet<TransportMode> modes = getTransportModes();

        ImmutableEnumSet<GraphLabel> results = GraphLabel.forModes(modes);
        assertEquals(ImmutableEnumSet.copyOf(GraphLabel.TransportModesLabels), results);
    }

    private static @NotNull ImmutableEnumSet<TransportMode> getTransportModes() {
        EnumSet<TransportMode> modes = EnumSet.allOf(TransportMode.class);
        modes.remove(TransportMode.Connect);
        modes.remove(TransportMode.NotSet);
        modes.remove(TransportMode.Unknown);
        modes.remove(TransportMode.Walk); // TODO inconsistency on use of QUERY_NODE for walks, needs sorting out
        return ImmutableEnumSet.copyOf(modes);
    }

}
