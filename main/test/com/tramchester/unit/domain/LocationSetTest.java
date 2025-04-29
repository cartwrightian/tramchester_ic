package com.tramchester.unit.domain;

import com.tramchester.domain.LocationSet;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class LocationSetTest {

    private Set<Station> stations;

    @BeforeEach
    void beforeEachTestRuns() {
        stations = new HashSet<>(Arrays.asList(Altrincham.fake(), Bury.fake(), Cornbrook.fake()));
    }

    private void assertListElementsPresent(LocationSet<Station> locationSet) {
        assertEquals(stations.size(), locationSet.size());

        assertTrue(locationSet.contains(Altrincham.fake()));
        assertTrue(locationSet.contains(Bury.fake()));
        assertTrue(locationSet.contains(Cornbrook.fake()));
    }

//    @Test
//    void shouldCreateSingleton() {
//        LocationSet<Station> locationSet = LocationSet.singleton(Altrincham.fake());
//
//        assertEquals(1, locationSet.size());
//        assertTrue(locationSet.contains(Altrincham.fake()));
//        assertFalse(locationSet.contains(StPetersSquare.fake()));
//    }

    @Test
    void shouldCreateFromSet() {
        Set<Station> stations = new HashSet<>(this.stations);

        LocationSet<Station> locationSet = new LocationSet<>(stations);

        assertListElementsPresent(locationSet);
        assertFalse(locationSet.contains(StPetersSquare.fake()));
    }

    @Test
    void shouldCollectStationsAsExpected() {

        Stream<Station> stream = stations.stream();

        LocationSet<Station> locationSet = stream.collect(LocationSet.stationCollector());

        assertListElementsPresent(locationSet);
        assertFalse(locationSet.contains(StPetersSquare.fake()));
    }

    @Test
    void shouldHaveAdd() {

        LocationSet<Station> locationSet = new LocationSet<>();

        assertTrue(locationSet.isEmpty());

        locationSet.add(Altrincham.fake());
        locationSet.add(Altrincham.fake());
        locationSet.add(Bury.fake());
        locationSet.add(Cornbrook.fake());

        assertListElementsPresent(locationSet);

    }

    @Test
    void shouldGetStationOnlyStream() {

        LocationSet<Station> locationSet = new LocationSet<>(stations);

        assertEquals(3, locationSet.size());

        Set<Station> result = locationSet.stream().collect(Collectors.toSet());

        assertEquals(3, result.size());

        assertTrue(result.contains(Altrincham.fake()));
        assertTrue(result.contains(Bury.fake()));
        assertTrue(result.contains(Cornbrook.fake()));
    }
}
