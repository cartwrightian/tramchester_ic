package com.tramchester.unit.domain;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.TestEnv.Modes.RailOnly;
import static org.junit.jupiter.api.Assertions.*;

public class TransportModeTest {

    @Test
    void intersectAsExpected() {
        ImmutableEnumSet<TransportMode> modesA = ImmutableEnumSet.of(Bus, Subway);
        ImmutableEnumSet<TransportMode> modesB = ImmutableEnumSet.of(Bus, Walk);
        ImmutableEnumSet<TransportMode> modesC = ImmutableEnumSet.of(Ship, Subway);

        assertTrue(modesA.anyIntersectionWith(modesB));
        assertFalse(modesB.anyIntersectionWith(modesC));
    }

    @Test
    void testSFromNumber() {
        assertEquals(Bus, TransportMode.fromNumber((short) 1));
        assertEquals(TransportMode.Unknown, TransportMode.fromNumber((short) 999));
    }

    @Test
    void shouldHaveVehicleModes() {
        assertFalse(Vehicles.contains(Walk));
        assertFalse(Vehicles.contains(Connect));
        assertFalse(Vehicles.contains(Unknown));
        assertFalse(Vehicles.contains(NotSet));

        long countVehicles = Arrays.stream(values()).filter(Vehicles::contains).count();
        assertEquals(7, countVehicles);

    }

    @Test
    void testSFromNumbers() {

        short[] numbers = { 1 , 4 , 8};
        ImmutableEnumSet<TransportMode> result = TransportMode.fromNumbers(numbers);

        assertEquals(ImmutableEnumSet.copyOf(EnumSet.of(Bus, Walk, Ship)), result);
    }

    @Test
    void parseFromCSV() {

        String text = "Walk,Bus,Ship";

        ImmutableEnumSet<TransportMode> result = TransportMode.parseCSV(text);

        assertEquals(ImmutableEnumSet.of(Bus, Walk, Ship), result);
    }

    @Test
    void shouldCheckForOverlaps() {
        assertFalse(TransportMode.TramsOnly.anyIntersectionWith(RailOnly));
    }

    @Test
    void shouldHaveSameSingleton() {
        assertSame(Walk.singleton(), Walk.singleton());
    }
}
