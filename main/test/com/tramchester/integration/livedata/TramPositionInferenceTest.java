package com.tramchester.integration.livedata;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.livedata.tfgm.TramPosition;
import com.tramchester.livedata.tfgm.TramPositionInference;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.RequiresNetwork;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.LiveDataDueTramsTest;
import com.tramchester.testSupport.testTags.LiveDataMessagesTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RequiresNetwork
class TramPositionInferenceTest {

    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig testConfig;

    private TramPositionInference positionInference;
    private StationRepository stationRepository;
    private LocalDateTime dateTime;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesLocalNow = new ProvidesLocalNow();

        LiveDataFetcher liveDataFetcher = componentContainer.get(LiveDataFetcher.class);

        RouteReachable routeReachable = componentContainer.get(RouteReachable.class);
        TramStationAdjacenyRepository adjacenyMatrix = componentContainer.get(TramStationAdjacenyRepository.class);
        TramDepartureRepository departureRepository = componentContainer.get(TramDepartureRepository.class);
        ClosedStationsRepository stationClosures = componentContainer.get(ClosedStationsRepository.class);
        TramchesterConfig config = componentContainer.get(TramchesterConfig.class);

        positionInference = new TramPositionInference(departureRepository, adjacenyMatrix, routeReachable,
                stationClosures, config);
        stationRepository = componentContainer.get(StationRepository.class);

        dateTime = providesLocalNow.getDateTime();

        liveDataFetcher.fetch();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    @LiveDataMessagesTest
    void needApiKeyPresentToFetchData() {
        assertNotNull(testConfig.getLiveDataConfig().getDataSubscriptionKey(), "subscription key null");
        assertFalse(testConfig.getLiveDataConfig().getDataSubscriptionKey().isEmpty(), "no subscription key present");
    }

    @Test
    @LiveDataMessagesTest
    @Disabled("needs a tram in the right place at the right time")
    void shouldInferTramPosition() {
        // NOTE: costs are not symmetric between two stations, i.e. one direction might cost more than the other
        // Guess: this is down to signalling, track, etc.

        Duration cost = Duration.ofMinutes(3); // cost between the stations, no due trams outside this limit should appear

        Station first = stationRepository.getStationById(TramStations.Deansgate.getId());
        Station second = stationRepository.getStationById(TramStations.Cornbrook.getId());

        StationPair pair = StationPair.of(first, second);
        TramPosition between = positionInference.findBetween(pair, dateTime);
        assertEquals(first, between.getFirst());
        assertEquals(second, between.getSecond());
        assertFalse(between.getTrams().isEmpty(), "no trams between");
        assertEquals(cost, between.getCost());

        TramTime now = TramTime.ofHourMins(TestEnv.LocalNow().toLocalTime());

        between.getTrams().
                forEach(dueTram -> assertFalse(dueTram.getWhen().isAfter(now), dueTram.getWhen().toString()));

        TramPosition otherDirection = positionInference.findBetween(pair, dateTime);
        assertFalse(otherDirection.getTrams().isEmpty(), "no trams in other direction");
        assertEquals(cost, between.getCost());
        otherDirection.getTrams().
                forEach(dueTram -> assertFalse(dueTram.getWhen().isAfter(now), dueTram.getWhen().toString()));
    }

    @Test
    @LiveDataDueTramsTest
    void shouldHaveSomeTramsPresentInNetwork() {
        List<TramPosition> tramPositions = positionInference.inferWholeNetwork(dateTime);

        assertFalse(tramPositions.isEmpty());

        List<TramPosition> results = tramPositions.stream().
                filter(TramPosition::hasTrams).
                toList();

        assertFalse(results.isEmpty(), tramPositions.toString());

    }
}
