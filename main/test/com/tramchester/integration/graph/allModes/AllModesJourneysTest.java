package com.tramchester.integration.graph.allModes;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.AllModesTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.rail.RailStationIds.ManchesterPiccadilly;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Victoria;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled("All mode planning is WIP")
public class AllModesJourneysTest {
    private static RouteCalculatorTestFacade routeCalculator;
    private static TramchesterConfig config;

    private static ComponentContainer componentContainer;
    private ImmutableGraphTransaction txn;
    private StationRepository stationRepository;
    private Duration maxJourneyDuration;
    private TramDate when;
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new AllModesTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        when = TestEnv.testDay();

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabaseNeo4J.class);

        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

        txn = graphDatabase.beginTx();
        routeCalculator = new RouteCalculatorTestFacade(componentContainer, txn);
        this.stationRepository = componentContainer.get(StationRepository.class);
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldHaveBusToTram() {
        StationLocalityGroup stockport = KnownLocality.Stockport.from(stationGroupsRepository);
        Station altyTram = stationRepository.getStationById(TramStations.Altrincham.getId());

        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest requestA = new JourneyRequest(when, travelTime, false, 2,
                maxJourneyDuration, 3, getRequestedModes());
        List<Journey> journeys = routeCalculator.calculateRouteAsList(stockport, altyTram, requestA);
        assertFalse(journeys.isEmpty());
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return config.getTransportModes();
    }

    @Test
    void shouldHaveBuryVictoriaTramJourney() {

        JourneyRequest request = new JourneyRequest(when,
                TramTime.of(11,53), false, 0, maxJourneyDuration, 1, getRequestedModes());

        List<Journey> journeys = routeCalculator.calculateRouteAsList(Bury, Victoria, request);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(Tram, stage.getMode());
        });
    }

    @Test
    void shouldHaveStockToAltyBusJourney() {

        StationLocalityGroup stockport = KnownLocality.Stockport.from(stationGroupsRepository);
        StationLocalityGroup alty = KnownLocality.Altrincham.from(stationGroupsRepository);

        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest requestA = new JourneyRequest(when, travelTime, false, 0,
                maxJourneyDuration, 3, getRequestedModes());
        List<Journey> journeys = routeCalculator.calculateRouteAsList(stockport, alty, requestA);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveStockportToManPiccRail() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 1,
                Duration.ofMinutes(30), 1, getRequestedModes());

        List<Journey> journeys = routeCalculator.calculateRouteAsList(RailStationIds.Stockport.getId(), ManchesterPiccadilly.getId(), request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).toList();
        assertFalse(direct.isEmpty(), "No direct from " + RailStationIds.Stockport + " to " + ManchesterPiccadilly);
    }

}
