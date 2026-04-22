package com.tramchester.unit.graph.core;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.reference.GraphLabels;
import com.tramchester.graph.reference.GraphLabelsFactory;
import com.tramchester.graph.reference.GraphLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.graph.reference.GraphLabel.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class GraphLabelsFactoryTest {
    private GraphLabelsFactory factory;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        factory = new GraphLabelsFactory();
    }

    @Test
    void shouldReturnSameLabelsSet() {
        ImmutableEnumSet<GraphLabel> originalA = ImmutableEnumSet.of(BUS, HOUR);
        ImmutableEnumSet<GraphLabel> originalB = ImmutableEnumSet.of(BUS, HOUR);

        GraphLabels resultA = factory.getFor(originalA);
        GraphLabels resultB = factory.getFor(originalB);

        assertSame(resultA, resultB);
    }

    @Test
    void shouldAddElement() {

        GraphLabels expected = factory.getFor(ImmutableEnumSet.of(BUS, HOUR, MINUTE));
        GraphLabels initial = factory.getFor(ImmutableEnumSet.of(BUS, HOUR));

        assertNotEquals(expected, initial);

        GraphLabels updated = factory.appendTo(initial, MINUTE);

        assertSame(expected, updated);
    }

}
