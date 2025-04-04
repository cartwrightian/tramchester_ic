package com.tramchester.integration.cloud.data;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.livedata.cloud.FindUniqueDueTramStatus;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.S3Test;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@S3Test
public class FindUniqueDueTramStatusTest {
    private static ComponentContainer componentContainer;
    private FindUniqueDueTramStatus finder;

    private final Set<String> expected = UpcomingDeparture.KNOWN_TRAM_STATUS;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig configuration = new DownloadsLiveDataFromS3Test.RealBucketConfig(
                new DownloadsLiveDataFromS3Test.RealLiveConfig("tramchesterlivedata","uat"));
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTest() {
        finder = componentContainer.get(FindUniqueDueTramStatus.class);
    }

    @Disabled("WIP")
    @Test
    void shouldHaveStandardStatus() {
        LocalDateTime start = LocalDateTime.of(2020, 2, 27, 0, 1);
        Duration duration = Duration.of(2, ChronoUnit.MINUTES);

        Duration sampleWindow = Duration.ofMinutes(1);

        Set<String> dueStatus = finder.getUniqueDueTramStatus(start, duration, sampleWindow);

        assertTrue(dueStatus.containsAll(expected), "Missing status in " + dueStatus + " does not contain all of " + expected);

    }

}
