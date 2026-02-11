package com.tramchester.unit.domain.collections;

import com.google.common.collect.Sets;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.reference.GraphLabel;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.*;
import static org.junit.jupiter.api.Assertions.*;

public class ImmutableEnumSetTest {

    @Test
    void shouldCreateWithOneItem() {
        ImmutableEnumSet<TransportMode> set = ImmutableEnumSet.of(Tram);

        ImmutableEnumSet<TransportMode> otherA = ImmutableEnumSet.of(Tram);
        assertEquals(set, otherA);

        ImmutableEnumSet<TransportMode> otherB = ImmutableEnumSet.of(Bus);
        assertNotEquals(set, otherB);

        assertEquals(1, set.size());
        assertFalse(set.isEmpty());
        assertTrue(set.contains(Tram));
        assertFalse(set.contains(Bus));

        assertTrue(set.anyIntersectionWith(ImmutableEnumSet.of(Tram)));
        assertTrue(set.anyIntersectionWith(ImmutableEnumSet.of(Bus,Tram)));
        assertFalse(set.anyIntersectionWith(ImmutableEnumSet.of(Bus,Ferry)));

        Sets.SetView<TransportMode> resultA = set.intersectionWith(ImmutableEnumSet.of(Bus, Ferry));
        assertTrue(resultA.isEmpty());

        Sets.SetView<TransportMode> resultB = set.intersectionWith(ImmutableEnumSet.of(Bus, Tram));
        assertEquals(1, resultB.size());
        assertTrue(resultB.contains(Tram));

        ImmutableEnumSet<GraphLabel> conversionResult = set.convertTo(GraphLabel.class, GraphLabel::forMode);
        assertEquals(1, conversionResult.size());
        assertTrue(conversionResult.contains(GraphLabel.TRAM));

        ImmutableEnumSet<TransportMode> withoutA = set.without(EnumSet.of(Ferry, Bus));
        assertEquals(set, withoutA);

        ImmutableEnumSet<TransportMode> withoutB = set.without(EnumSet.of(Ferry, Tram));
        assertTrue(withoutB.isEmpty());

        EnumSet<TransportMode> resultAdd = EnumSet.of(Bus);
        set.addAllTo(resultAdd);
        assertEquals(2, resultAdd.size());
        assertTrue(resultAdd.contains(Bus));
        assertTrue(resultAdd.contains(Tram));

    }

    @Test
    void shouldHandleTransitionOneToManyAndVisaVersa() {
        ImmutableEnumSet<TransportMode> set = ImmutableEnumSet.of(Tram);

        EnumSet<TransportMode> enumSet = ImmutableEnumSet.createEnumSet(set);
        assertEquals(1, enumSet.size());
        assertTrue(enumSet.contains(Tram));

        ImmutableEnumSet<TransportMode> mergeSame = ImmutableEnumSet.join(set, set);
        assertEquals(1, mergeSame.size());

        ImmutableEnumSet<TransportMode> mergeDiff = ImmutableEnumSet.join(set, ImmutableEnumSet.of(Bus, Ferry));
        assertEquals(3, mergeDiff.size());

        ImmutableEnumSet<TransportMode> largerSet = ImmutableEnumSet.of(Tram, Ferry, Bus);

        ImmutableEnumSet<TransportMode> removedResult = largerSet.without(EnumSet.of(Tram, Ferry));
        assertEquals(1, removedResult.size());
        assertTrue(removedResult.contains(Bus));
    }

    @Test
    void shouldCreateWithOneManyItem() {
        ImmutableEnumSet<TransportMode> set = ImmutableEnumSet.of(Tram, Walk, RailReplacementBus);

        ImmutableEnumSet<TransportMode> otherA = ImmutableEnumSet.of(Tram, Walk, RailReplacementBus);
        assertEquals(set, otherA);

        ImmutableEnumSet<TransportMode> otherB = ImmutableEnumSet.of(Bus, Ferry);
        assertNotEquals(set, otherB);

        assertEquals(3, set.size());
        assertFalse(set.isEmpty());
        assertTrue(set.contains(Tram));
        assertTrue(set.contains(Walk));
        assertTrue(set.contains(RailReplacementBus));
        assertFalse(set.contains(Bus));

        assertTrue(set.anyIntersectionWith(ImmutableEnumSet.of(Tram)));
        assertTrue(set.anyIntersectionWith(ImmutableEnumSet.of(Bus,Tram)));
        assertFalse(set.anyIntersectionWith(ImmutableEnumSet.of(Bus,Ferry)));

        Sets.SetView<TransportMode> resultA = set.intersectionWith(ImmutableEnumSet.of(Bus, Ferry));
        assertTrue(resultA.isEmpty());

        Sets.SetView<TransportMode> resultB = set.intersectionWith(ImmutableEnumSet.of(Bus, Tram));
        assertEquals(1, resultB.size());
        assertTrue(resultB.contains(Tram));

        ImmutableEnumSet<GraphLabel> conversionResult = set.convertTo(GraphLabel.class, GraphLabel::forMode);
        assertEquals(3, conversionResult.size());
        assertTrue(conversionResult.contains(GraphLabel.TRAM));


        ImmutableEnumSet<TransportMode> withoutA = set.without(EnumSet.of(Ferry, Bus));
        assertEquals(set, withoutA);

        ImmutableEnumSet<TransportMode> withoutB = set.without(EnumSet.of(Ferry, Tram));
        assertEquals(2, withoutB.size());
        assertFalse(withoutB.contains(Tram));

        EnumSet<TransportMode> resultAdd = EnumSet.of(Bus);
        set.addAllTo(resultAdd);
        assertEquals(4, resultAdd.size());
        assertTrue(resultAdd.contains(Bus));
        assertTrue(resultAdd.contains(Tram));
        assertTrue(resultAdd.contains(Walk));
        assertTrue(resultAdd.contains(RailReplacementBus));

    }

}
