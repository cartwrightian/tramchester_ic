package com.tramchester.unit.domain;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
    void shouldHaveVehicleModes() {
        assertFalse(Vehicles.contains(Walk));
        assertFalse(Vehicles.contains(Connect));
        assertFalse(Vehicles.contains(Unknown));
        assertFalse(Vehicles.contains(NotSet));

        long countVehicles = Arrays.stream(values()).filter(Vehicles::contains).count();
        assertEquals(7, countVehicles);

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

    @Test
    void shouldConsolidateReplacementServices() {
        assertEquals(Tram.singleton(), TransportMode.consolidateReplacements(ImmutableEnumSet.of(TramReplacementBus)));
        assertEquals(Train.singleton(), TransportMode.consolidateReplacements(ImmutableEnumSet.of(RailReplacementBus)));

        assertEquals(Ferry.singleton(), TransportMode.consolidateReplacements(ImmutableEnumSet.of(Ferry)));

        ImmutableEnumSet<TransportMode> transportModes = ImmutableEnumSet.of(TramReplacementBus, Tram, Ferry);
        assertEquals(transportModes.without(TramReplacementBus.singleton()), TransportMode.consolidateReplacements(transportModes));

    }
}
