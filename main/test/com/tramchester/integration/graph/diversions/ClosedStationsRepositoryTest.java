package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.StationClosuresConfigForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
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

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        when = TestEnv.testDay();
        overlap = when.plusDays(3);

        Set<TramStations> diversionOnly = Collections.emptySet();
        StationClosuresConfigForTest closureA = new StationClosuresConfigForTest(StPetersSquare, new DateRange(when, when.plusWeeks(1)),
                true);
        StationClosuresConfigForTest closureB = new StationClosuresConfigForTest(TraffordCentre, new DateRange(overlap, when.plusWeeks(2)),
                false, diversionOnly, Collections.emptySet());
        StationClosuresConfigForTest closureC = new StationClosuresConfigForTest(ExchangeSquare, new DateRange(overlap, when.plusWeeks(3)),
                false, Collections.singleton(Victoria), Collections.emptySet());

        // overlaps with A
        StationClosuresConfigForTest closureD = new StationClosuresConfigForTest(PiccadillyGardens, new DateRange(when, when.plusWeeks(1)),
                true);

        List<StationClosures> closedStations = Arrays.asList(closureA, closureB, closureC, closureD);

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
        afterClosures = when.plusWeeks(4);
    }

    @Test
    void shouldUseProvidedDiversions() {
        Set<ClosedStation> closed = closedStationsRepository.getClosedStationsFor(DataSourceID.tfgm);
        List<ClosedStation> closedIn3Weeks = closed.stream().
                filter(closedStation -> closedStation.getDateTimeRange().contains(when.plusWeeks(3))).toList();
        assertEquals(1, closedIn3Weeks.size());

        ClosedStation closure = closedIn3Weeks.getFirst();

        List<Station> aroundClosure = new ArrayList<>(closure.getDiversionAroundClosure());
        assertEquals(1, aroundClosure.size());

        Station divert = aroundClosure.getFirst();
        assertEquals(TramStations.Victoria.getId(), divert.getId());
    }

    @Test
    void shouldHaveExpectedClosedStationsForFirstPeriod() {
        Set<ClosedStation> closed = closedStationsRepository.getAnyWithClosure(when);
        assertEquals(2, closed.size());
        IdSet<Station> ids = closed.stream().map(ClosedStation::getStationId).collect(IdSet.idCollector());
        assertTrue(ids.contains(StPetersSquare.getId()));
        assertTrue(ids.contains(PiccadillyGardens.getId()));

        Set<ClosedStation> closedLater = closedStationsRepository.getAnyWithClosure(afterClosures);
        assertTrue(closedLater.isEmpty());
    }

    @Test
    void shouldHaveExpectedClosedStationsForOverlap() {
        Set<ClosedStation> fullyClosed = closedStationsRepository.getAnyWithClosure(overlap);
        assertEquals(4, fullyClosed.size());
        IdSet<Station> ids = fullyClosed.stream().map(ClosedStation::getStationId).collect(IdSet.idCollector());
        assertTrue(ids.contains(StPetersSquare.getId()));
    }

    @Test
    void shouldHaveNearbyStationsForClosed() {
        List<ClosedStation> closedStations = new ArrayList<>(closedStationsRepository.getAnyWithClosure(when.plusDays(1)));
        assertEquals(2, closedStations.size());

        Optional<ClosedStation> maybeStPeters = closedStations.stream().
                filter(closed -> closed.getStationId().equals(StPetersSquare.getId())).findFirst();
        assertTrue(maybeStPeters.isPresent());

        ClosedStation closedStation = maybeStPeters.get();

        IdSet<Station> diversionsAround = closedStation.getDiversionAroundClosure().stream().collect(IdSet.collector());
        assertFalse(diversionsAround.isEmpty());

        assertTrue(diversionsAround.contains(Deansgate.getId()));
        assertFalse(diversionsAround.contains(PiccadillyGardens.getId())); // overlap
        assertFalse(diversionsAround.contains(ExchangeSquare.getId())); // overlap

        IdSet<Station> diversionsToFrom = closedStation.getDiversionAroundClosure().stream().collect(IdSet.collector());
        assertFalse(diversionsToFrom.isEmpty());

        assertTrue(diversionsToFrom.contains(Deansgate.getId()));
        assertFalse(diversionsToFrom.contains(PiccadillyGardens.getId())); // overlap
        assertFalse(diversionsToFrom.contains(ExchangeSquare.getId())); // overlap

    }


}
