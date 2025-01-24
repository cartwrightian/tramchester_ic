package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RailAndTramStationRepositoryTest {
    private static ComponentContainer componentContainer;
    private StationRepository stationRepository;
    private CRSRepository crsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        RailAndTramGreaterManchesterConfig config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        crsRepository = componentContainer.get(CRSRepository.class);
    }

    @Test
    void shouldHaveConsistencyOnCRSAndStationRepositoryForInBoundsStation() {
        Station hale = RailStationIds.Hale.from(stationRepository);

        String crs = crsRepository.getCRSFor(hale.getId());

        Station fromCRS = crsRepository.getFor(crs);

        assertEquals(hale, fromCRS);
    }

    @Test
    void shouldHaveGetFromCRSAndStationRepositoryInBoundsStation() {
        Station hale = crsRepository.getFor("HAL");
        assertTrue(stationRepository.hasStationId(hale.getId()));
    }

    @Disabled("This can in fact happen as only in-use and inbound stations are added to main station repository")
    @Test
    void shouldHaveGetFromCRSAndStationRepositoryForOutofBoundsStation() {
        Station chester = crsRepository.getFor("CTR");
        assertTrue(stationRepository.hasStationId(chester.getId()));
    }
}
