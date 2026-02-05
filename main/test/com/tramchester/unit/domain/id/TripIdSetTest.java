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
        TripIdSet tripIdSet = TripIdSet.Factory.singleton(tripA);

        assertFalse(tripIdSet.isEmpty());
        assertEquals(1, tripIdSet.size());
        assertTrue(tripIdSet.contains(tripA));
        assertFalse(tripIdSet.contains(Trip.createId("tripB")));
    }

    @Test
    void shouldCreateASetAndAppend() {
        IdFor<Trip> tripA = Trip.createId("tripA");
        TripIdSet tripIdSet = TripIdSet.Factory.singleton(tripA);

        IdFor<Trip> tripB = Trip.createId("tripB");
        assertFalse(tripIdSet.contains(tripB));

        TripIdSet updated = TripIdSet.Factory.copyThenAppend(tripIdSet, tripB);
        assertTrue(updated.contains(tripB));
    }

    @Test
    void shouldAppendIds() {
        TripIdSet tripIdSetA = TripIdSet.Factory.empty();

        int number = 10000;
        for (int i = 0; i < number; i++) {
            final IdFor<Trip> tripId = Trip.createId("trip" + i);
            tripIdSetA = TripIdSet.Factory.copyThenAppend(tripIdSetA, tripId);
        }

        assertEquals(number, tripIdSetA.size());

        TripIdSet tripIdSetB = TripIdSet.Factory.empty();

        for (int i = 0; i < number; i++) {
            final IdFor<Trip> tripId = Trip.createId("trip" + i);
            tripIdSetB = TripIdSet.Factory.copyThenAppend(tripIdSetB,tripId);
        }

        assertEquals(number, tripIdSetB.size());


    }
}
