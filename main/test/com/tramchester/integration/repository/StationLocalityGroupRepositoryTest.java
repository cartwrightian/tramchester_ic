package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithGroupsEnabled;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StationLocalityGroupRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private StationGroupsRepository stationGroupsRepository;
    private StationRepository stationRepository;

    // NOTE: currently (3/2024) most tram stations are not allocated to a local area in Naptan

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        final TramchesterConfig config = new IntegrationTramTestConfigWithGroupsEnabled();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldBeEnabled() {
        assertTrue(stationGroupsRepository.isEnabled());

        Set<StationLocalityGroup> loaded = stationGroupsRepository.getStationGroupsFor(TransportMode.Tram);

        assertEquals(17, loaded.size());
    }

    @Test
    void shouldHaveExpectedTramStationGroup() {
        StationLocalityGroup found = stationGroupsRepository.getStationGroupForArea(KnownLocality.ManchesterCityCentre.getLocalityId());

        assertNotNull(found);

        LocationSet<Station> locations = found.getAllContained();

        assertEquals(5, locations.size(), HasId.asIds(locations));

        assertTrue(locations.contains(TramStations.ExchangeSquare.from(stationRepository)));
        assertTrue(locations.contains(TramStations.Deansgate.from(stationRepository)));
        assertTrue(locations.contains(TramStations.StPetersSquare.from(stationRepository)));
        assertTrue(locations.contains(TramStations.Victoria.from(stationRepository)));
        assertTrue(locations.contains(TramStations.Piccadilly.from(stationRepository)));

        assertFalse(locations.contains(TramStations.MarketStreet.from(stationRepository)));
        assertFalse(locations.contains(TramStations.Shudehill.from(stationRepository)));

    }

    @Test
    void shouldFindGroupByName() {
        StationLocalityGroup found = stationGroupsRepository.findByName("Sale");

        assertNotNull(found);

        assertEquals(3, found.getAllContained().size());
    }


}
