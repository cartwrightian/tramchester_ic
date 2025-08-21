package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.LocationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.tramchester.testSupport.reference.TramStations.Victoria;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
public class LocationRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private LocationRepository locationRepository;
    private TramchesterConfig config;
    private boolean naptanEnabled;

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
        naptanEnabled = config.hasRemoteDataSourceConfig(DataSourceID.naptanxml);
    }

    @Test
    void shouldFindAStation() {
        Location<?> result = locationRepository.getLocation(Victoria.getLocationId());
        assertEquals(Victoria.getId(), result.getId());
    }

    @Test
    void shouldFindStationFromDTOId() {
        Location<?> result = locationRepository.getLocation(LocationType.Station, Victoria.getIdForDTO());
        assertEquals(Victoria.getId(), result.getId());
    }

    @Test
    void shouldCheckIfLocationPresent() {
        assertTrue(locationRepository.hasLocation(LocationType.Station, Victoria.getIdForDTO()));

        IdFor<StationLocalityGroup> cityCentreId = KnownLocality.ManchesterCityCentre.getId();

        assertEquals(naptanEnabled, locationRepository.hasLocation(LocationType.StationGroup, IdForDTO.createFor(cityCentreId)));
    }

    @Test
    void shouldCheckIfLocationPMissing() {
        assertFalse(locationRepository.hasLocation(LocationType.Platform, Victoria.getIdForDTO()));
    }
}
