package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
public class StationLocationsRepositoryTest {
    private static ComponentContainer componentContainer;

    private StationLocationsRepository locationsRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        locationsRepository = componentContainer.get(StationLocationsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldFindNearStations() {
        Station station = PiccadillyGardens.from(stationRepository);
        List<Station> results = locationsRepository.nearestStationsSorted(station, 5,
                MarginInMeters.ofMeters(600), Tram.singleton());

        assertEquals(5, results.size());

        assertEquals(station, results.get(0));
        assertEquals(MarketStreet.from(stationRepository), results.get(1));
        assertEquals(StPetersSquare.from(stationRepository), results.get(2));
        assertEquals(Piccadilly.from(stationRepository), results.get(3));
        assertEquals(Shudehill.from(stationRepository), results.get(4));
    }

    @Test
    void shouldFindNearLocation() {

        List<Station> results = locationsRepository.nearestStationsSorted(KnownLocations.nearMarketStreet.location(), 5,
                MarginInMeters.ofMeters(600), Tram.singleton());

        assertEquals(5, results.size());

        assertEquals(MarketStreet.from(stationRepository), results.get(0));
        assertEquals(PiccadillyGardens.from(stationRepository), results.get(1));
        assertEquals(StPetersSquare.from(stationRepository), results.get(2));
        assertEquals(ExchangeSquare.from(stationRepository), results.get(3));
        assertEquals(Shudehill.from(stationRepository), results.get(4));
    }

    @Test
    void shouldFindNearStationsUnsorted() {
        Station station = PiccadillyGardens.from(stationRepository);
        List<Station> tramResults = locationsRepository.nearestStationsUnsorted(station, MarginInMeters.ofMeters(600)).
                filter(item -> item.getTransportModes().contains(Tram)).toList();

        assertEquals(5, tramResults.size());

        assertTrue(tramResults.contains(station), "Did not find in " + HasId.asIds(tramResults));
        assertTrue(tramResults.contains(Piccadilly.from(stationRepository)), "Did not find in " + HasId.asIds(tramResults));
        assertTrue(tramResults.contains(MarketStreet.from(stationRepository)), "Did not find in " + HasId.asIds(tramResults));
        assertTrue(tramResults.contains(StPetersSquare.from(stationRepository)), "Did not find in " + HasId.asIds(tramResults));
        assertTrue(tramResults.contains(Shudehill.from(stationRepository)), "Did not find in " + HasId.asIds(tramResults));
    }
}
