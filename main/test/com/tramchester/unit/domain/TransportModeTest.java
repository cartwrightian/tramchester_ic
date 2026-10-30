package com.tramchester.unit.domain;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.tramchester.domain.reference.TransportMode.*;
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
        assertEquals(8, countVehicles);

        // not vehicle - alerts if new value added to the enum
        assertEquals(4, values().length-countVehicles);

    }

    @Test
    void parseFromCSV() {

        String text = "Walk,Bus,Ship";

        ImmutableEnumSet<TransportMode> result = TransportMode.parseCSV(text);

        assertEquals(ImmutableEnumSet.of(Bus, Walk, Ship), result);
    }

    @Test
    void shouldCheckForOverlaps() {
        assertFalse(TransportMode.TramsOnly.anyIntersectionWith(TrainOnly));
    }

    @Test
    void shouldHaveSameSingleton() {
        assertSame(Walk.singleton(), Walk.singleton());
    }

    @Test
    void shouldHaveReplacementBus() {
        long countReplacements = Arrays.stream(values()).filter(TransportMode::isReplacementBus).count();
        assertEquals(2, countReplacements);

        long notReplacements = Arrays.stream(values()).filter(item -> !isReplacementBus(item)).count();
        assertEquals(10, notReplacements);
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
