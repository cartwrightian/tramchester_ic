package com.tramchester.integration.dataimport.rail;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.rail.RailDataFilenameRepository;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataUpdateTest
public class RailDataFilenameRepositoryTest {

    public static final String CURRENT_VERSION = "477";

    private static GuiceContainerDependencies componentContainer;
    private RailDataFilenameRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationRailTestConfig config = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.GreaterManchester);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(RailDataFilenameRepository.class);
    }

    @Test
    void shouldGetVersionNumber() {
        String result = repository.getCurrentVersion();
        assertEquals(CURRENT_VERSION, result);
    }

    @Test
    void shouldGetTimetablePath() {
        Path result = repository.getTimetable();

        Path expected = Path.of("data/openRailData", RailDataFilenameRepository.PREFIX + CURRENT_VERSION + RailDataFilenameRepository.TIMETABLE_FILE_EXT);

        assertEquals(expected, result);
    }

    @Test
    void shouldGetStationsPath() {
        Path result = repository.getStations();

        Path expected = Path.of("data/openRailData", RailDataFilenameRepository.PREFIX + CURRENT_VERSION + RailDataFilenameRepository.STATIONS_FILE_EXT);

        assertEquals(expected, result);
    }

}
