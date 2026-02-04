package com.tramchester.unit.domain.id;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.TripIdSet;
import com.tramchester.domain.input.Trip;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TripIdSetTest {

    @Test
    void shouldCreateASet() {
        IdFor<Trip> tripA = Trip.createId("tripA");
        TripIdSet tripIdSet = TripIdSet.singleton(tripA);

        assertFalse(tripIdSet.isEmpty());
        assertEquals(1, tripIdSet.size());
        assertTrue(tripIdSet.contains(tripA));
        assertFalse(tripIdSet.contains(Trip.createId("tripB")));
    }

    @Test
    void shouldCreateASetAndAppend() {
        IdFor<Trip> tripA = Trip.createId("tripA");
        TripIdSet tripIdSet = TripIdSet.singleton(tripA);

        IdFor<Trip> tripB = Trip.createId("tripB");
        assertFalse(tripIdSet.contains(tripB));

        TripIdSet updated = tripIdSet.copyThenAppend(tripB);
        assertTrue(updated.contains(tripB));
    }

    @Test
    void shouldAppendIds() {
        TripIdSet tripIdSetA = TripIdSet.empty();

        int number = 10000;
        for (int i = 0; i < number; i++) {
            final IdFor<Trip> tripId = Trip.createId("trip" + i);
            tripIdSetA = tripIdSetA.copyThenAppend(tripId);
        }

        assertEquals(number, tripIdSetA.size());

        TripIdSet tripIdSetB = TripIdSet.empty();

        for (int i = 0; i < number; i++) {
            final IdFor<Trip> tripId = Trip.createId("trip" + i);
            tripIdSetB = tripIdSetB.copyThenAppend(tripId);
        }

        assertEquals(number, tripIdSetB.size());


    }
}
