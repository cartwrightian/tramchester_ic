package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationRepositoryBusTest {
    private StationRepository stationRepository;

    private static ComponentContainer componentContainer;

    // "Agecroft Road" -> one example of the issue, so during import enhance names from naptan mean should not
    // get any duplicate names
    public static final List<String> agecroftRoadStops = Arrays.asList("1800NF40681", "1800NF40691", "1800WF39051", "1800WF39061");

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
    }


    @Test
    void shouldNotDuplicateNames() {

        IdSet<Station> stopIds = agecroftRoadStops.stream().map(Station::createId).collect(IdSet.idCollector());

        assertEquals(4, stopIds.size());

        IdSet<Station> missing = stopIds.stream().filter(id -> !stationRepository.hasStationId(id)).collect(IdSet.idCollector());

        assertTrue(missing.isEmpty(), "missing " + missing);

        Set<String> uniqueNames = stopIds.stream().
                map(id -> stationRepository.getStationById(id)).
                map(Station::getName).collect(Collectors.toSet());

        assertEquals(4, uniqueNames.size(), "wrong number unique names " + uniqueNames);
    }
}
