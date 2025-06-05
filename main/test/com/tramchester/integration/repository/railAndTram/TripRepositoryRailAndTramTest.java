package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
class TripRepositoryRailAndTramTest {
    private static ComponentContainer componentContainer;
    private TripRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(TripRepository.class);
    }

    @Test
    void shouldGetTripsWithSensibleLength() {
        Set<Trip> trips = repository.getTrips();

        IdSet<Trip> tooShort = trips.stream().filter(trip -> trip.getStopCalls().totalNumber() <= 1)
                .collect(IdSet.collector());

        assertTrue(tooShort.isEmpty(), tooShort.toString());
    }

    @Test
    void shouldHaveActiveStartStationsForAllTrips() {
        Set<Trip> trips = repository.getTrips();

        Set<Trip> inactiveStarts = trips.stream().
                filter(trip -> !isActive(trip, t -> t.getStopCalls().getFirstStop(true))).
                collect(Collectors.toSet());

        assertTrue(inactiveStarts.isEmpty(), HasId.asIds(inactiveStarts));
    }

    @Test
    void shouldHaveActiveLastStationsForAllTrips() {
        Set<Trip> trips = repository.getTrips();

        Set<Trip> inactiveLast = trips.stream().
                filter(trip -> !isActive(trip, t -> t.getStopCalls().getLastStop(true))).
                collect(Collectors.toSet());

        assertTrue(inactiveLast.isEmpty(), HasId.asIds(inactiveLast));
    }

    boolean isActive(Trip trip, Function<Trip, StopCall> getStopcall) {
        return getStopcall.apply(trip).getStation().isActive();
    }

   
}
