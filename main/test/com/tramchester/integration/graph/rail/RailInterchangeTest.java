package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
public class RailInterchangeTest {
    private static GuiceContainerDependencies componentContainer;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;
    private RouteInterchangeRepository routeInterchanges;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeInterchanges = componentContainer.get(RouteInterchangeRepository.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveExpectedInterchange() {
        Station manPicc = stationRepository.getStationById(RailStationIds.ManchesterPiccadilly.getId());
        assertTrue(interchangeRepository.isInterchange(manPicc));

        Station euston = stationRepository.getStationById(RailStationIds.LondonEuston.getId());
        assertTrue(interchangeRepository.isInterchange(euston));
    }

    @Test
    void shouldNotHaveInterchangeIfRoutesAreGrouped() {
        Station hale = stationRepository.getStationById(RailStationIds.Hale.getId());
        assertFalse(interchangeRepository.isInterchange(hale));
    }

    @Disabled("no longer compute cost, it was inaccurate")
    @Test
    void shouldHaveConsistencyOnZeroCostToInterchangeAndInterchanges() {
        Set<RouteStation> zeroCostToInterchange = stationRepository.getRouteStations().stream().
                //filter(routeStation -> routeInterchanges.costToInterchange(routeStation).isZero()).
                filter(routeStation -> routeInterchanges.hasPathToInterchange(routeStation)).
                collect(Collectors.toSet());

        Set<RouteStation> zeroCostButNotInterchange = zeroCostToInterchange.stream().
                filter(zeroCost -> !interchangeRepository.isInterchange(zeroCost.getStation())).
                collect(Collectors.toSet());

        assertTrue(zeroCostButNotInterchange.isEmpty(), HasId.asIds(zeroCostButNotInterchange));
    }



}
