package com.tramchester.unit.graph.core;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.collections.ImmutableEnumSetImpl;
import com.tramchester.graph.core.inMemory.NodeLabelsFactory;
import com.tramchester.graph.reference.GraphLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.graph.reference.GraphLabel.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class NodeLabelsFactoryTest {
    private NodeLabelsFactory factory;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        factory = new NodeLabelsFactory();
    }

    @Test
    void shouldReturnSameLabelsSet() {
        ImmutableEnumSet<GraphLabel> originalA = ImmutableEnumSetImpl.of(BUS, HOUR);
        ImmutableEnumSet<GraphLabel> originalB = ImmutableEnumSetImpl.of(BUS, HOUR);

        ImmutableEnumSet<GraphLabel> resultA = factory.getFor(originalA);
        ImmutableEnumSet<GraphLabel> resultB = factory.getFor(originalB);

        assertSame(resultA, resultB);
    }

    @Test
    void shouldAddElement() {

        ImmutableEnumSet<GraphLabel> expected = factory.getFor(ImmutableEnumSet.of(BUS, HOUR, MINUTE));
        ImmutableEnumSet<GraphLabel> initial = factory.getFor(ImmutableEnumSet.of(BUS, HOUR));

        assertNotEquals(expected, initial);

        ImmutableEnumSet<GraphLabel> updated = factory.appendTo(initial, MINUTE);

        assertSame(expected, updated);
    }

}
