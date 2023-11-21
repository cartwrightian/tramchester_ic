package com.tramchester.unit.domain;

import com.tramchester.domain.reference.TransportMode;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.*;
import static org.junit.jupiter.api.Assertions.*;

public class TransportModeTest {

    @Test
    void intersectAsExpected() {
        EnumSet<TransportMode> modesA = EnumSet.of(Bus, Subway);
        EnumSet<TransportMode> modesB = EnumSet.of(Bus, Walk);
        EnumSet<TransportMode> modesC = EnumSet.of(Ship, Subway);

        assertTrue(TransportMode.intersects(modesA, modesB));
        assertFalse(TransportMode.intersects(modesB, modesC));
    }

    @Test
    void testSFromNumber() {
        assertEquals(Bus, TransportMode.fromNumber((short) 1));
        assertEquals(TransportMode.Unknown, TransportMode.fromNumber((short) 999));
    }

    @Test
    void testSFromNumbers() {

        short[] numbers = { 1 , 4 , 8};
        EnumSet<TransportMode> result = TransportMode.fromNumbers(numbers);

        assertEquals(EnumSet.of(Bus, Walk, Ship), result);
    }

    @Test
    void parseFromCSV() {

        String text = "Walk,Bus,Ship";

        EnumSet<TransportMode> result = TransportMode.parseCSV(text);

        assertEquals(EnumSet.of(Bus, Walk, Ship), result);

    }
}
