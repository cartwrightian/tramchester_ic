package com.tramchester.integration.cloud.data;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DownloadedRemotedDataRepository;
import com.tramchester.deployment.UploadRemoteSourceData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.RequiresNetwork;
import com.tramchester.testSupport.testTags.S3Test;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresNetwork
@S3Test
class UploadRemoteSourceDataToS3Test {

    private static S3Client s3;
    private static S3Waiter s3Waiter;
    private static S3TestSupport s3TestSupport;

    private static GuiceContainerDependencies componentContainer;
    private static UploadRemoteSourceData uploadRemoteData;

    private final static String TEST_BUCKET_NAME = "tramchestertestlivedatabucket";
    private static DataSource dataSource;
    private static DownloadedRemotedDataRepository downloadedRemoteData;

    @BeforeAll
    static void beforeAnyDone() {

        dataSource = new DataSource();
        TramchesterConfig configuration = new IntegrationTestBucketConfig(TEST_BUCKET_NAME, dataSource);
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        s3 = S3Client.builder().build();
        s3Waiter = S3Waiter.create();

        s3TestSupport = new S3TestSupport(s3, s3Waiter, configuration.getDistributionBucket());
        s3TestSupport.createOrCleanBucket();

        uploadRemoteData = componentContainer.get(UploadRemoteSourceData.class);

        downloadedRemoteData = componentContainer.get(DownloadedRemotedDataRepository.class);
    }

    @AfterAll
    static void afterAllDone() {
        componentContainer.close();

        s3TestSupport.deleteBucket();
        s3Waiter.close();
        s3.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        s3TestSupport.cleanBucket();
    }

    @AfterEach
    void afterEachTestRuns() {
        s3TestSupport.cleanBucket();
    }

    @Test
    void shouldUploadOkIfBucketExist() throws IOException {

        // Can't check actual remote URL, so make sure the files is not "expired" by creating a new file

        Path sourceFilePath = dataSource.getDownloadPath().resolve(dataSource.getDownloadFilename());
        Files.deleteIfExists(sourceFilePath);

        downloadedRemoteData.addFileFor(DataSourceID.tfgm, sourceFilePath);

        final String testPrefix = "testing/testPrefix";
        final String key = testPrefix +"/"+ dataSource.getDownloadFilename();

        final String text = "HereIsSomeTextForTheFile";
        Files.writeString( sourceFilePath, text);

        ZonedDateTime modTime = ZonedDateTime.of(1975, 12, 30, 15, 35, 56, 0, ZoneOffset.UTC);
        long expectedMills = getEpochMilli(modTime);
        assertTrue(sourceFilePath.toFile().setLastModified(expectedMills));

        boolean result = uploadRemoteData.upload(testPrefix);
        assertTrue(result);

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).build());

        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).build();
        HeadObjectResponse response = s3.headObject(headObjectRequest);

        assertTrue(response.hasMetadata());
        Map<String, String> meta = response.metadata();
        assertTrue(meta.containsKey(ClientForS3.ORIG_MOD_TIME_META_DATA_KEY));
        String dateAsText = meta.get(ClientForS3.ORIG_MOD_TIME_META_DATA_KEY);

        ZonedDateTime remoteModTime = ZonedDateTime.parse(dateAsText, ClientForS3.DATE_TIME_FORMATTER);
        assertEquals(modTime, remoteModTime);

        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).build();
        ResponseInputStream<GetObjectResponse> inputStream = s3.getObject(getRequest);

        byte[] buffer = new byte[text.length()];
        int readSize = inputStream.read(buffer);
        inputStream.close();

        String remoteContents = new String(buffer);
        assertEquals(remoteContents, text);
        assertEquals(readSize, text.length());

        boolean repeatedUpload = uploadRemoteData.upload(testPrefix);
        assertTrue(repeatedUpload, "overwrite failed");
    }

    public static class IntegrationTestBucketConfig extends TestConfig {

        private final String bucketName;
        private final RemoteDataSourceConfig dataSource;

        public IntegrationTestBucketConfig(String bucketName, RemoteDataSourceConfig dataSource) {
            this.bucketName = bucketName;
            this.dataSource = dataSource;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public String getDistributionBucket() {
            return bucketName;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(dataSource);
        }
    }

    private long getEpochMilli(ZonedDateTime dateTime) {
//        ZonedDateTime zonedDateTime = dateTime.atZone(TramchesterConfig.TimeZoneId);
        return dateTime.toInstant().toEpochMilli();
    }

    public static class DataSource extends RemoteDataSourceConfig {

        @Override
        public Path getDataPath() {
            return Path.of("data", "test");
        }

        @Override
        public Path getDownloadPath() {
            return getDataPath();
        }

        @Override
        public String getDataCheckUrl() {
            return "";
        }

        @Override
        public String getDataUrl() {
            throw new RuntimeException("Should not be downloading, not expired");
        }

        @Override
        public boolean checkOnlyIfExpired() {
            return false;
        }

        @Override
        public Duration getDefaultExpiry() {
            return Duration.ofDays(1);
        }

        @Override
        public String getDownloadFilename() {
            return "testFile.txt";
        }

        @Override
        public String getName() {
            return "tfgm";
        }

        @Override
        public String getModTimeCheckFilename() {
            return "";
        }
    }



}
