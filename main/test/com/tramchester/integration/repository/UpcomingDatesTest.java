package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

@Disabled("WIP")
@DataUpdateTest
public class UpcomingDatesTest {
    private static GuiceContainerDependencies componentContainer;
    private StopCallRepository stopCallRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stopCallRepository = componentContainer.get(StopCallRepository.class);
    }

//    @Disabled("WIP")
//    @Test
//    void shouldHaveExpectedClosures() {
//        List<IdFor<Station>> expectedClosed = stopCallRepository.getClosedBetween(Eccles.getId(), Broadway.getId());
//
//        DateRange range = UpcomingDates.MediaCityEcclesWorks2025;
//
//        range.stream().forEach(date -> {
//            IdSet<Station> missing = expectedClosed.stream().filter(closed -> !UpcomingDates.hasClosure(closed, date)).
//                    collect(IdSet.idCollector());
//            assertTrue(missing.isEmpty(), "On " + date + " still open " + missing);
//        });
//
//    }
}
