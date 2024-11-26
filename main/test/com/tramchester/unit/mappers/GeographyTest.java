package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.mappers.Geography;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static org.junit.jupiter.api.Assertions.*;
import static tech.units.indriya.unit.Units.*;

public class GeographyTest {
    private Geography geography;
    private TramchesterConfig config;

    @BeforeEach
    void onceBeforeEachTest() {
        config = TestEnv.GET();
        geography = new Geography(config);
    }

    public static final Quantity<Length> BetweenStPeterSqAndPiccGardens = Quantities.getQuantity(463.7D, METRE);

    @Test
    void shouldGetWalkingTime() {

        Location<?> start = TramStations.StPetersSquare.fake();
        Location<?> end = TramStations.PiccadillyGardens.fake();

        Duration expected = TestEnv.calcCostInMinutes(start, end, config.getWalkingMPH());

        Quantity<Time> result = geography.getWalkingTime(BetweenStPeterSqAndPiccGardens);

        Number seconds = result.to(SECOND).getValue();

        assertEquals(Duration.ofMinutes(5).plusSeconds(45), expected, "earth quake??");

        long diff = Math.abs(seconds.longValue() - expected.toSeconds());

        assertTrue(diff<2, seconds + " and " + expected + " too far apart");

    }

    @Test
    void shouldReproIssueOnDistanceMismatch() {
        Location<?> start = nearAltrincham.location();
        Location<?> end = Altrincham.fake();

        Duration result = geography.getWalkingDuration(start, end);

        Duration expected = TestEnv.calcCostInMinutes(start, end, config.getWalkingMPH());

        long diff = Math.abs(result.toSeconds() - expected.toSeconds());

        assertTrue(diff < 5L, expected + " and " + result + " too far apart");
        //assertEquals(expected, result, expected + " and " + result + " need to match");

    }

    @Test
    void shouldGetWalkingTimeInMins() {

        Location<?> start = TramStations.StPetersSquare.fake();
        Location<?> end = TramStations.PiccadillyGardens.fake();

        int expectedSeconds = 346;

        Duration result = geography.getWalkingDuration(start, end);

        assertEquals(expectedSeconds, result.getSeconds());
    }

    @Test
    void shouldGetDistanceBetweenLocations() {
        Station stPeters = TramStations.StPetersSquare.fake();
        Station piccGardens = TramStations.PiccadillyGardens.fake();

        Quantity<Length> distance = geography.getDistanceBetweenInMeters(stPeters, piccGardens);

        assertEquals(BetweenStPeterSqAndPiccGardens.getValue().doubleValue(),
                distance.getValue().doubleValue(), 0.1);
    }

    @Test
    void shouldGetNearToLocationSorted() {
        Station stationA = Altrincham.fake();
        Station stationB = TramStations.PiccadillyGardens.fake();
        Station stationC = TramStations.Shudehill.fake();

        MyLocation myLocation = new MyLocation(nearAltrincham.latLong());

        Geography.LocationsSource<Station> provider = () -> Stream.of(stationC, stationA, stationB);

        List<Station> results = geography.
                getNearToSorted(provider, myLocation.getGridPosition(), MarginInMeters.ofMeters(20000)).collect(Collectors.toList());

        assertEquals(3, results.size());
        assertEquals(stationA, results.get(0));
        assertEquals(stationB, results.get(1));
        assertEquals(stationC, results.get(2));
    }

    @Test
    void shouldGetBoundary() {
        Station stationA = Altrincham.fake();
        Station stationB = TramStations.PiccadillyGardens.fake();
        Station stationC = TramStations.Shudehill.fake();

        List<LatLong> result = geography.createBoundaryFor(Stream.of(stationA, stationB, stationC).map(Location::getLatLong));

        assertFalse(result.isEmpty());

        // todo how to test this?
    }
}
