package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.StationClosuresConfigForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class ClosedStationsRepositoryTest {
    // Note this needs to be > time for whole test fixture, see note below in @After

    private static ComponentContainer componentContainer;
    private static IntegrationTramClosedStationsTestConfig config;
    private static TramDate when;
    private static TramDate overlap;

    private ClosedStationsRepository closedStationsRepository;

    private TramDate afterClosures;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        when = TestEnv.testDay();
        overlap = when.plusDays(3);

        Set<String> diversionOnly = Collections.emptySet();
        StationClosuresConfigForTest closureA = new StationClosuresConfigForTest(StPetersSquare, when, when.plusWeeks(1), true, diversionOnly);
        StationClosuresConfigForTest closureB = new StationClosuresConfigForTest(TraffordCentre, overlap, when.plusWeeks(2), false, diversionOnly);
        StationClosuresConfigForTest closureC = new StationClosuresConfigForTest(TramStations.ExchangeSquare, overlap, when.plusWeeks(3),
                false, Collections.singleton("9400ZZMAVIC"));

        List<StationClosures> closedStations = Arrays.asList(closureA, closureB, closureC);

        config = new IntegrationTramClosedStationsTestConfig(closedStations, true);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        afterClosures = when.plusWeeks(4);
    }

    @Test
    void shouldHaveAnyOpenCheckWhenNotClosedStation() {
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(TramStations.Altrincham.from(stationRepository));
        assertTrue(closedStationsRepository.anyStationOpen(locations, when));
    }

    @Test
    void shouldHaveAnyOpenCheckWhenClosed() {
        LocationSet<Station> locations = new LocationSet<>(Collections.singletonList(StPetersSquare.from(stationRepository)));
        assertFalse(closedStationsRepository.anyStationOpen(locations, when));
    }

    @Test
    void shouldHaveAnyOpenCheckWhenNotFullyClosed() {
        LocationSet<Station> locations = new LocationSet<>(Arrays.asList(TraffordCentre.from(stationRepository), ExchangeSquare.from(stationRepository)));
        assertTrue(closedStationsRepository.anyStationOpen(locations, overlap));
    }

    @Test
    void shouldHaveAnyOpenCheckWhenNotClosedIncluded() {
        LocationSet<Station> locations = new LocationSet<>(Arrays.asList(StPetersSquare.from(stationRepository), Bury.from(stationRepository)));
        assertTrue(closedStationsRepository.anyStationOpen(locations, when));
    }

    @Test
    void shouldHaveAnyOpenCheckAfterClosure() {
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(TramStations.Altrincham.from(stationRepository));
        assertTrue(closedStationsRepository.anyStationOpen(locations, afterClosures));
    }

    @Test
    void shouldUseProvidedDiversions() {
        Set<ClosedStation> closed = closedStationsRepository.getClosedStationsFor(DataSourceID.tfgm);
        List<ClosedStation> closedIn3Weeks = closed.stream().filter(closedStation -> closedStation.getDateRange().contains(when.plusWeeks(3))).toList();
        assertEquals(1, closedIn3Weeks.size());

        ClosedStation closure = closedIn3Weeks.get(0);
        List<Station> nearby = new ArrayList<>(closure.getNearbyLinkedStation());

        assertEquals(1, nearby.size());

        Station divert = nearby.get(0);
        assertEquals(TramStations.Victoria.getId(), divert.getId());
    }

    @Test
    void shouldBeInEffectOnExpectedDays() {
        assertTrue(closedStationsRepository.hasClosuresOn(when));
        assertTrue(closedStationsRepository.hasClosuresOn(overlap));
        assertTrue(closedStationsRepository.hasClosuresOn(when.plusWeeks(2)));
        assertFalse(closedStationsRepository.hasClosuresOn(when.plusWeeks(6)));
    }

    @Test
    void shouldHaveExpectedClosedStationsForFirstPeriod() {
        Set<ClosedStation> closed = closedStationsRepository.getFullyClosedStationsFor(when);
        assertEquals(1, closed.size());
        IdSet<Station> ids = closed.stream().map(ClosedStation::getStation).collect(IdSet.collector());
        assertTrue(ids.contains(StPetersSquare.getId()));

        Set<ClosedStation> closedLater = closedStationsRepository.getFullyClosedStationsFor(afterClosures);
        assertTrue(closedLater.isEmpty());
    }

    @Test
    void shouldHaveExpectedClosedStationsForSecondPeriod() {
        Set<ClosedStation> fullyClosed = closedStationsRepository.getFullyClosedStationsFor(when.plusWeeks(2));
        assertTrue(fullyClosed.isEmpty());
    }

    @Test
    void shouldHaveExpectedClosedStationsForOverlap() {
        Set<ClosedStation> fullyClosed = closedStationsRepository.getFullyClosedStationsFor(overlap);
        assertEquals(1, fullyClosed.size());
        IdSet<Station> ids = fullyClosed.stream().map(ClosedStation::getStation).collect(IdSet.collector());
        assertTrue(ids.contains(StPetersSquare.getId()));
    }

    @Test
    void shouldHaveClosedByDataSourceId() {
        Set<ClosedStation> closedStations = closedStationsRepository.getClosedStationsFor(DataSourceID.tfgm);
        assertEquals(3, closedStations.size());
    }

    @Test
    void shouldHaveNearbyStationsForClosed() {
        List<ClosedStation> closedStations = new ArrayList<>(closedStationsRepository.getFullyClosedStationsFor(when.plusDays(1)));
        assertEquals(1, closedStations.size());

        ClosedStation closedStation = closedStations.get(0);
        assertEquals(StPetersSquare.getId(), closedStation.getStation().getId());

        IdSet<Station> availableStations = closedStation.getNearbyLinkedStation().stream().collect(IdSet.collector());
        assertFalse(availableStations.isEmpty());

        assertTrue(availableStations.contains(Deansgate.getId()));
        assertTrue(availableStations.contains(PiccadillyGardens.getId()));
        assertTrue(availableStations.contains(MarketStreet.getId()));


    }

}
