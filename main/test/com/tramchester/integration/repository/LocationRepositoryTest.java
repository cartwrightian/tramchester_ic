package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.places.Location;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.LocationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.tramchester.domain.places.LocationType.Platform;
import static com.tramchester.domain.places.LocationType.Station;
import static com.tramchester.testSupport.reference.TramStations.Victoria;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
public class LocationRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private LocationRepository locationRepository;
    private TramchesterConfig config;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        config = componentContainer.get(TramchesterConfig.class);
        locationRepository = componentContainer.get(LocationRepository.class);
    }

    @Test
    void shouldFindAStation() {
        Location<?> result = locationRepository.getLocation(Victoria.getLocationId());
        assertEquals(Victoria.getId(), result.getId());
    }

    @Test
    void shouldFindStationFromDTOId() {
        Location<?> result = locationRepository.getLocation(Station, Victoria.getIdForDTO());
        assertEquals(Victoria.getId(), result.getId());
    }

    @Test
    void shouldCheckIfLocationPresent() {
        boolean naptanEnabled = config.hasRemoteDataSourceConfig(DataSourceID.naptanxml);

        assumeTrue(naptanEnabled);

        assertTrue(locationRepository.hasLocation(Station, Victoria.getIdForDTO()));

//        IdFor<StationLocalityGroup> stationLocationGroup = KnownLocality.Shudehill.getId();
//
//        StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
//        assertTrue(stationGroupsRepository.hasGroup(stationLocationGroup), "No group found in group repos for "
//                + stationLocationGroup);
//
//        assertTrue(locationRepository.hasLocation(LocationType.StationGroup, IdForDTO.createFor(stationLocationGroup)),
//                "No group found for " + stationLocationGroup);
    }

    @Test
    void shouldCheckIfLocationPMissing() {
        assertFalse(locationRepository.hasLocation(Platform, Victoria.getIdForDTO()));
    }
}
