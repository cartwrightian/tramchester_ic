package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.KnownLocations.betweenPiccAndPiccGardens;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
@DataUpdateTest
public class RailAndTramStationLocationsRepositoryTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig tramchesterConfig;

    private StationLocations locationsRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        tramchesterConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        locationsRepository = componentContainer.get(StationLocations.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveTramAndTrain() {
        LocationSet<Station> withinBounds = locationsRepository.getStationsWithin(tramchesterConfig.getBounds());

        ImmutableEnumSet<TransportMode> modes = withinBounds.stream().flatMap(item -> item.getTransportModes().stream()).
                collect(ImmutableEnumSet.collect(TransportMode.class));

        assertEquals(ImmutableEnumSet.of(Tram, Train, RailReplacementBus), modes);
    }

    @Test
    void shouldFindByAreaId() {
        // NPTGLocality:E0057786
        LocationCollection found = locationsRepository.getLocationsWithin(NPTGLocality.createId("E0057786"));

        assertEquals(92, found.size());
        assertEquals(ImmutableEnumSet.of(Tram, Train, RailReplacementBus), found.getModes());

        assertTrue(found.contains(RailStationIds.ManchesterPiccadilly.getLocationId()));
        assertTrue(found.contains(Piccadilly.getLocationId()));
    }

    @Test
    void shouldFindNearTrainStations() {
        Station station = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        List<Station> results = locationsRepository.nearestStationsSorted(station, 5,
                MarginInMeters.ofMeters(600), ImmutableEnumSet.of(Tram, Train));

        assertEquals(3, results.size());

        assertEquals(station, results.get(0));
        assertEquals(Piccadilly.from(stationRepository), results.get(1));
        assertEquals(PiccadillyGardens.from(stationRepository), results.get(2));
    }

    @Test
    void shouldFindNearStations() {
        Station station = Piccadilly.from(stationRepository);
        List<Station> results = locationsRepository.nearestStationsSorted(station, 5,
                MarginInMeters.ofMeters(600), ImmutableEnumSet.of(Tram, Train));

        assertEquals(3, results.size());

        assertEquals(station, results.get(0));
        assertEquals(RailStationIds.ManchesterPiccadilly.from(stationRepository), results.get(1));
        assertEquals(PiccadillyGardens.from(stationRepository), results.get(2));
    }

    @Test
    void shouldFindNearLocation() {

        List<Station> results = locationsRepository.nearestStationsSorted(betweenPiccAndPiccGardens.location(), 5,
                MarginInMeters.ofMeters(650), ImmutableEnumSet.of(Tram, Train));

        assertEquals(5, results.size());

        assertEquals(PiccadillyGardens.from(stationRepository), results.get(0));
        assertEquals(MarketStreet.from(stationRepository), results.get(1));
        assertEquals(RailStationIds.ManchesterPiccadilly.from(stationRepository), results.get(2));
        assertEquals(Piccadilly.from(stationRepository), results.get(3));
        assertEquals(StPetersSquare.from(stationRepository), results.get(4));
    }

    @Test
    void shouldFindNearStationsUnsorted() {
        Station station = Piccadilly.from(stationRepository);
        List<Station> results = locationsRepository.nearestStationsUnsorted(station, MarginInMeters.ofMeters(600)).toList();

        assertEquals(3, results.size());

        assertTrue(results.contains(station), "Did not find in " + HasId.asIds(results));
        assertTrue(results.contains(Piccadilly.from(stationRepository)), "Did not find in " + HasId.asIds(results));
        assertTrue(results.contains(PiccadillyGardens.from(stationRepository)), "Did not find in " + HasId.asIds(results));
        assertTrue(results.contains(RailStationIds.ManchesterPiccadilly.from(stationRepository)), "Did not find in " + HasId.asIds(results));
    }
}
