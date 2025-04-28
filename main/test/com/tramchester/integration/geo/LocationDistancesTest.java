package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.LocationDistances;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocationDistancesTest {

    private static ComponentContainer componentContainer;
    private LocationDistances locationDistances;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        locationDistances = componentContainer.get(LocationDistances.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldGetExpectedDistancesForAltyToNavRoad() {
        long expectedMeters = 1030;

        LocationCollection destinations = LocationCollectionSingleton.of(NavigationRoad.from(stationRepository));

        long result = locationDistances.findDistancesTo(destinations).toStation(TramStations.Altrincham.getId());

        assertEquals(expectedMeters, result);

    }

    @Test
    void shouldGetExpectedDistanceBuryToPiccadillyArea() {
        long expectedMeters = 12719L;

        Set<Station> aroundPiccadilly = Stream.of(Piccadilly, PiccadillyGardens, MarketStreet).
                map(item -> item.from(stationRepository)).collect(Collectors.toSet());

        LocationSet<Station> locationSet = LocationSet.of(aroundPiccadilly);
        long result = locationDistances.findDistancesTo(locationSet).toStation(TramStations.Bury.getId());

        assertEquals(expectedMeters, result);
    }

    @Test
    void shouldHaveDistanceForAllStations() {
        // can afford to do this for trams, not that many stations

        List<Station> all = stationRepository.getActiveStationStream().toList();

        int count = all.size();

        Stream<StationPair> pairs = all.stream().flatMap(stationA -> all.stream().map(stationB -> StationPair.of(stationA, stationB)));

        long foundOk = pairs.map(pair -> locationDistances.findDistancesTo(LocationCollectionSingleton.of(pair.getBegin())).toStation(pair.getEnd().getId())).
                filter(result -> result != Long.MAX_VALUE).count();

        long expected = (long) count * count;

        assertEquals(expected, foundOk);
    }
}
