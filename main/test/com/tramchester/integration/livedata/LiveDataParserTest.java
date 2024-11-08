package com.tramchester.integration.livedata;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.Location;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.tfgm.LiveDataParser;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LiveDataParserTest {

    private static GuiceContainerDependencies componentContainer;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {

        // NOTE: Actual load of live data is disabled here as right now just checking static mappings

        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Disabled);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveRealStationNamesForDestinationMapping() {
        Set<String> validNames = stationRepository.getStations().stream().
                map(Location::getName).collect(Collectors.toSet());

        Set<LiveDataParser.LiveDataNamesMapping> missing = Arrays.stream(LiveDataParser.LiveDataNamesMapping.values()).
                filter(mapping -> !validNames.contains(mapping.getToo())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), missing.toString());
    }
}
