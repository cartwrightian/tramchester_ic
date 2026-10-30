package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ServicesForLocationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static org.junit.jupiter.api.Assertions.*;

public class ServicesForLocationRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private StationRepository stationRepository;
    private ServicesForLocationRepository repository;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        repository = componentContainer.get(ServicesForLocationRepository.class);
        when = TestEnv.testDay();
    }

    @Test
    void shouldCheckForOverlappingModes() {
        Station station = TramStations.Piccadilly.from(stationRepository);
        LocationId<Station> locationId = station.getLocationId();

        assertTrue(repository.modesOverlap(TramsOnly, locationId));
        assertFalse(repository.modesOverlap(TransportMode.Bus.singleton(), locationId));
    }

    @Test
    void shouldHaveServicesForPiccadilly() {
        Station station = TramStations.Piccadilly.from(stationRepository);
        LocationId<Station> locationId = station.getLocationId();

        Set<Service> services = repository.getFor(locationId);

        assertFalse(services.isEmpty());

        Set<Service> running = services.stream().
                filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        assertFalse(running.isEmpty());

        assertTrue(repository.isAvailable(when, locationId));

    }

    @Test
    void shouldHaveServicesForNavigationRoad() {
        Station station = TramStations.NavigationRoad.from(stationRepository);

        Set<Service> services = repository.getFor(station.getLocationId());

        assertFalse(services.isEmpty());

        Set<Service> running = services.stream().
                filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        boolean expected = !running.isEmpty();

        assertEquals(expected, repository.isAvailable(when, station.getLocationId()));

    }
}
