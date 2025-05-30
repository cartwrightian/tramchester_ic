package com.tramchester.integration.cloud.data;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.cloud.DownloadsLiveDataFromS3;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestTramLiveDataConfig;
import com.tramchester.testSupport.testTags.LiveDataS3UploadTest;
import com.tramchester.testSupport.testTags.S3Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LiveDataS3UploadTest
@S3Test
class DownloadsLatestLiveDataFromS3Test {
    private static final int NUM_OF_DISPLAYS = 189;

    private static ComponentContainer componentContainer;
    private DownloadsLiveDataFromS3 downloader;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig configuration = new RealBucketConfig(new RealLiveConfig("tramchesterlivedata","ProdGreen"));
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTest() {
        downloader = componentContainer.get(DownloadsLiveDataFromS3.class);
    }

    @Test
    void shouldDownloadNewestData() {
        LocalDateTime start = TestEnv.LocalNow().minusHours(1);
        Duration duration = Duration.ofMinutes(1);

        List<ArchivedStationDepartureInfoDTO> results = downloader.downloadFor(start, duration).toList();

        assertFalse(results.isEmpty());
        assertTrue(results.size() >= NUM_OF_DISPLAYS, "Len was " + results.size());
    }

    private static class RealBucketConfig extends IntegrationTramTestConfig {
        private final TfgmTramLiveDataConfig liveDataConfig;

        private RealBucketConfig(TfgmTramLiveDataConfig liveDataConfig) {
            this.liveDataConfig = liveDataConfig;
        }

        @Override
        public TfgmTramLiveDataConfig getLiveDataConfig() {
            return liveDataConfig;
        }
    }

    private static class RealLiveConfig extends TestTramLiveDataConfig {
        private final String bucketName;
        private final String prefix;

        public RealLiveConfig(String bucketName, String prefix) {
            this.bucketName = bucketName;
            this.prefix = prefix;
        }

        @Override
        public String getS3Bucket() {
            return bucketName;
        }

        @Override
        public String getS3Prefix() {
            return prefix;
        }
    }

}
