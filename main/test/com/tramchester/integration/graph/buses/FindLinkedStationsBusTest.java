package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@BusTest
class FindLinkedStationsBusTest {

    private static ComponentContainer componentContainer;
    private FindLinkedStations findStationLinks;
    private StationGroup shudehillCentralBusStops;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        findStationLinks = componentContainer.get(FindLinkedStations.class);
        BusStations.CentralStops centralStops = new BusStations.CentralStops(componentContainer);

        shudehillCentralBusStops = centralStops.Shudehill();
    }

    @Test
    void shouldHaveCorrectTransportMode() {
        Set<StationToStationConnection> forBus = findStationLinks.findLinkedFor(Bus);
        long notBus = forBus.stream().filter(link -> !link.getLinkingModes().contains(Bus)).count();
        assertEquals(0, notBus);

        long isBus = forBus.stream().filter(link -> link.getLinkingModes().contains(Bus)).count();
        assertEquals(forBus.size(), isBus);
    }

    @Test
    void shouldGetStationNeighboursFromTram() {
        IdSet<Station> busStopIds = shudehillCentralBusStops.getContained().stream().collect(IdSet.collector());

        Set<StationToStationConnection> links = findStationLinks.findLinkedFor(Bus);

        assertFalse(links.isEmpty());

        Set<StationToStationConnection> beginAtShudehill = links.stream().
                filter(link ->  busStopIds.contains(link.getBegin().getId())).
                collect(Collectors.toSet());

        assertFalse(beginAtShudehill.isEmpty());

    }

}
