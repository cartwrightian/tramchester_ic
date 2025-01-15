package com.tramchester.unit.domain.id;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LocationIdTest {

    IdFor<Station> stationIdA = Station.createId("1234AAA");
    IdFor<Station> stationIdB = Station.createId("5678BBB");

    @Test
    void shouldBeEquals() {
        LocationId<Station> locationIdA = new LocationId<>(stationIdA);
        LocationId<Station> locationIdB = new LocationId<>(stationIdA);

        assertTrue(locationIdA.equals(locationIdA));
        assertTrue(locationIdA.equals(locationIdB));
        assertTrue(locationIdB.equals(locationIdA));
    }

    @Test
    void shouldNotBeEquals() {
        LocationId<Station> locationIdA = new LocationId<>(stationIdA);
        LocationId<Station> locationIdB = new LocationId<>(stationIdB);

        assertFalse(locationIdA.equals(locationIdB));
        assertFalse(locationIdB.equals(locationIdA));
    }

    @Test
    void shouldBeEqualsToUnderlyingId() {
        LocationId<Station> locationIdA = new LocationId<>(stationIdA);

        assertEquals(locationIdA.getId(), stationIdA);
    }
}
