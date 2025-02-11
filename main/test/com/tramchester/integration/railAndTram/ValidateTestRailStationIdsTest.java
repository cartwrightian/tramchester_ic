package com.tramchester.integration.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
class ValidateTestRailStationIdsTest {
    private static ComponentContainer componentContainer;
    private StationRepository stationRepository;
    private CRSRepository crsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();
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
    void shouldHaveTestStationsInTheRepository() {
        Set<RailStationIds> missing = Arrays.stream(RailStationIds.values()).
                filter(RailStationIds::isGreaterManchester).
                filter(id -> !stationRepository.hasStationId(id.getId())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), missing.toString());

    }

    @Test
    void shouldHaveTestStationsInTheCRSRepository() {
        Set<RailStationIds> missing = Arrays.stream(RailStationIds.values()).
                filter(station -> !crsRepository.hasCRSCode(station.crs())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldHaveCRSAndStationIdConsistency() {
        Set<RailStationIds> mismatch = Arrays.stream(RailStationIds.values()).
                filter(station -> !crsRepository.getCRSCodeFor(station.getId()).equals(station.crs())).
                collect(Collectors.toSet());

        assertTrue(mismatch.isEmpty(), mismatch.toString());
    }

}
