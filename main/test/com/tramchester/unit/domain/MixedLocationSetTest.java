package com.tramchester.unit.domain;

import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLocations.nearShudehill;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class MixedLocationSetTest {
    private Set<Station> stations;
    private MyLocation location;

    @BeforeEach
    void beforeEachTestRuns() {
        stations = new HashSet<>(Arrays.asList(Altrincham.fake(), Bury.fake(), Cornbrook.fake()));
        location = nearShudehill.location();
    }

    private void assertListElementsPresent(MixedLocationSet locationSet) {
        assertEquals(stations.size(), locationSet.size());

        assertTrue(locationSet.contains(Altrincham.getLocationId()));
        assertTrue(locationSet.contains(Bury.getLocationId()));
        assertTrue(locationSet.contains(Cornbrook.getLocationId()));
    }

    @Test
    void shouldCreateFromSet() {
        Set<Station> stations = new HashSet<>(this.stations);

        MixedLocationSet locationSet = new MixedLocationSet(stations);

        assertListElementsPresent(locationSet);
        assertFalse(locationSet.contains(StPetersSquare.getLocationId()));
    }

    @Test
    void shouldGetMixedStream() {

        MixedLocationSet locationSet = new MixedLocationSet();

        locationSet.add(location); // new MixedLocationSet();

        assertEquals(1, locationSet.size());

        locationSet.addAll(new LocationSet<>(stations));

        Set<Location<?>> result = locationSet.locationStream().collect(Collectors.toSet());

        assertEquals(4, result.size());

        assertTrue(result.contains(location));
        assertTrue(result.contains(Altrincham.fake()));
        assertTrue(result.contains(Bury.fake()));
        assertTrue(result.contains(Cornbrook.fake()));
    }

    @Test
    void shouldGetStationOnlyStream() {

        LocationSet<Station> locationSet = new LocationSet<>(stations);

//        locationSet.add(location);

        assertEquals(3, locationSet.size());

        Set<Station> result = locationSet.stream().collect(Collectors.toSet());

        assertEquals(3, result.size());

        assertTrue(result.contains(Altrincham.fake()));
        assertTrue(result.contains(Bury.fake()));
        assertTrue(result.contains(Cornbrook.fake()));
    }
}
