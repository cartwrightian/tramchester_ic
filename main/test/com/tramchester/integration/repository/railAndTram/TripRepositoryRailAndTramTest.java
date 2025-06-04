package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
class TripRepositoryRailAndTramTest {
    private static ComponentContainer componentContainer;
    private TripRepository repository;

    // TODO this code is only used for test support

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
    void shouldHaveActiveStartStationsForAllTrips() {
        Set<Trip> trips = repository.getTrips();

        Set<Trip> inactiveStarts = trips.stream().
                filter(trip -> isActive(trip, t -> t.getStopCalls().getFirstStop())).
                collect(Collectors.toSet());

        assertTrue(inactiveStarts.isEmpty(), HasId.asIds(inactiveStarts));
    }

    @Test
    void shouldHaveActiveLastStationsForAllTrips() {
        Set<Trip> trips = repository.getTrips();

        Set<Trip> inactiveLast = trips.stream().
                filter(trip -> isActive(trip, t -> t.getStopCalls().getLastStop())).
                collect(Collectors.toSet());

        assertTrue(inactiveLast.isEmpty(), HasId.asIds(inactiveLast));
    }

    boolean isActive(Trip trip, Function<Trip, StopCall> getStopcall) {
        return getStopcall.apply(trip).getStation().isActive();
    }

   
}
