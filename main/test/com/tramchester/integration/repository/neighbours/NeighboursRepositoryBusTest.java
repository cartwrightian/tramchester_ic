package com.tramchester.integration.repository.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.integration.testSupport.config.IntegrationTramBusTestConfig;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.TramBusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TramBusTest
public class NeighboursRepositoryBusTest {

    private NeighboursRepository neighboursRepository;
    private StationGroupsRepository stationGroupsRepository;

    private StationLocalityGroup shudehillGroup;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramBusTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        neighboursRepository = componentContainer.get(NeighboursRepository.class);

        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        shudehillGroup = KnownLocality.Shudehill.from(stationGroupsRepository);
        shudehillTram = stationRepository.getStationById(Shudehill.getId());
    }

    @Test
    void shouldHaveCorrectNeighboursForAltrinchamTram() {
        StationLocalityGroup altrinchamGroup = KnownLocality.Altrincham.from(stationGroupsRepository);
        IdSet<Station> centralIds = altrinchamGroup.getAllContained().
                stream().filter(Station::isCentral).
                collect(IdSet.collector());

        IdSet<Station> neighbours = neighboursRepository.getNeighboursFor(TramStations.Altrincham.getId())
                .stream().collect(IdSet.collector());


        assertTrue(neighbours.containsAll(centralIds));
    }

    @Test
    void shouldHaveCorrectNeighboursForTramAtShudehill() {
        IdSet<Station> neighbours = neighboursRepository.getNeighboursFor(Shudehill.getId()).stream().collect(IdSet.collector());
        IdSet<Station> busStops = shudehillGroup.getAllContained().stream().collect(IdSet.collector());
        assertTrue(neighbours.containsAll(busStops));
    }

    @Test
    void shouldHaveCorrectNeighboursForBusAtShudehill() {
        shudehillGroup.getAllContained().stream().forEach(station -> {
            IdSet<Station> neighbours = neighboursRepository.getNeighboursFor(station.getId()).stream().collect(IdSet.collector());
            assertTrue(neighbours.contains(shudehillTram.getId()));
        });
    }

}
