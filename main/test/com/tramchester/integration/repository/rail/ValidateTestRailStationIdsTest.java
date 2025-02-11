package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
class ValidateTestRailStationIdsTest {
    private static ComponentContainer componentContainer;
    private StationRepository stationRepository;
    private CRSRepository crsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationRailTestConfig config = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.National);
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
        for (int i = 0; i < RailStationIds.values().length; i++) {
            RailStationIds testId = RailStationIds.values()[i];
            assertTrue(stationRepository.hasStationId(testId.getId()), testId + " is missing");
        }
    }

    @Test
    void shouldHaveMatchingCRS() {
        List<RailStationIds> incorrect = Arrays.stream(RailStationIds.values()).
                filter(item -> !crsRepository.getCRSCodeFor(item.getId()).equals(item.crs())).
                toList();

        List<Pair<String, String>> diag = incorrect.stream().
                map(item -> Pair.of(crsRepository.getCRSCodeFor(item.getId()), item.name() + ": " + item.crs())).
                toList();

        assertTrue(incorrect.isEmpty(), diag.toString());

    }

}
