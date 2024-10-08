package com.tramchester.integration.cloud.data;

import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.OpenLdbConfig;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.dataexport.Zipper;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestOpenLdbConfig;
import com.tramchester.testSupport.TestTramLiveDataConfig;
import com.tramchester.testSupport.testTags.LiveDataS3UploadTest;
import com.tramchester.testSupport.testTags.S3Test;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@LiveDataS3UploadTest
@S3Test
class LiveDataClientForS3Test {

    private static final String TEST_BUCKET_NAME = "tramchestertestlivedatabucketnew";
    private static final String PREFIX = "test";
    private static final String FULL_KEY = PREFIX+"/"+ "key";

    private static S3Client s3;
    private static S3Waiter s3Waiter;
    private static S3TestSupport s3TestSupport;
    private static ClientForS3 clientForS3;

    private LiveDataClientForS3 liveDataClientForS3;

    @BeforeAll
    static void beforeAnyDone() {
        s3 = S3Client.builder().build();
        s3Waiter = S3Waiter.create();

        s3TestSupport = new S3TestSupport(s3, s3Waiter, TEST_BUCKET_NAME);
        s3TestSupport.createOrCleanBucket();

        Zipper zipper = new Zipper();
        clientForS3 = new ClientForS3(new GetsFileModTime(), zipper);
        clientForS3.start();
    }

    @AfterAll
    static void afterAllDone() {
        clientForS3.stop();
        s3TestSupport.deleteBucket();
        s3Waiter.close();
        s3.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        liveDataClientForS3 = new LiveDataClientForS3(new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled), clientForS3);
        liveDataClientForS3.start();
        s3TestSupport.cleanBucket();
    }

    @AfterEach
    void afterEachTestRuns() {
        liveDataClientForS3.stop();
        s3TestSupport.cleanBucket();
    }

    @Test
    void shouldUploadOkIfBucketExist() throws IOException {

        String contents = "someJsonData";
        boolean uploaded = liveDataClientForS3.upload(FULL_KEY, contents, TestEnv.UTCNow());
        assertTrue(uploaded, "uploaded");

        ListObjectsRequest listRequest = ListObjectsRequest.builder().bucket(TEST_BUCKET_NAME).build();
        ListObjectsResponse currentContents = s3.listObjects(listRequest);

        List<S3Object> summary = currentContents.contents();

        assertEquals(1, summary.size());
        String key = summary.get(0).key();

        assertEquals("test/key", key);

        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(FULL_KEY).build();
        ResponseInputStream<GetObjectResponse> inputStream = s3.getObject(getRequest);

        byte[] target = new byte[contents.length()];
        inputStream.read(target);
        inputStream.close();

        String result = new String(target);
        assertEquals(contents, result);
    }

    @Test
    void shouldReturnFalseIfNonExistentBucket() {
        LiveDataClientForS3 anotherClient = new LiveDataClientForS3(new NoSuchBucketExistsConfig(), clientForS3);
        anotherClient.start();
        boolean uploaded = anotherClient.upload(FULL_KEY, "someText", TestEnv.UTCNow());
        anotherClient.stop();
        assertFalse(uploaded);
    }

    @Test
    void checkForObjectExisting() {
        HeadObjectRequest existsCheckRequest = HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(FULL_KEY).build();

        PutObjectRequest request = PutObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(FULL_KEY).build();
        s3.putObject(request, RequestBody.fromString("contents"));
        s3Waiter.waitUntilObjectExists(existsCheckRequest);

        assertTrue(liveDataClientForS3.itemExists(PREFIX, "key"), "exists"); //waiter will throw if times out
        Set<String> keys = liveDataClientForS3.getKeysFor(PREFIX).collect(Collectors.toSet());
        assertTrue(keys.contains(FULL_KEY));

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(FULL_KEY).build();
        s3.deleteObject(deleteRequest);
        s3Waiter.waitUntilObjectNotExists(existsCheckRequest);

        assertFalse(liveDataClientForS3.itemExists(PREFIX, "key"), "deleted");
    }

    @Test
    void shouldDownloadFromBucketNew() throws NoSuchAlgorithmException {
        String payload = "contents";
        String key = FULL_KEY + "B";
        HeadObjectRequest existsCheckRequest = HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).build();

        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] bytes = payload.getBytes();
        String localMd5 = Base64.encodeBase64String(messageDigest.digest(bytes));

        PutObjectRequest request = PutObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).contentMD5(localMd5).build();
        s3.putObject(request, RequestBody.fromString(payload));
        s3Waiter.waitUntilObjectExists(existsCheckRequest);

        LiveDataClientForS3.ResponseMapper<String> transformer = (receivedKey, receivedBytes) ->
                Collections.singletonList(new String(receivedBytes, StandardCharsets.US_ASCII));

        List<String> results = liveDataClientForS3.downloadAndMap(Collections.singleton(key), transformer).toList();
        assertEquals(1, results.size());
        assertEquals(payload, results.get(0));
    }

    private static class NoSuchBucketExistsConfig extends TestConfig {

        @Override
        public TfgmTramLiveDataConfig getLiveDataConfig() {
            return new LiveDataConfigNoSuchBuket();
        }

        @Override
        public OpenLdbConfig getOpenldbwsConfig() {
            return new TestOpenLdbConfig();
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.emptyList();
        }

        private static class LiveDataConfigNoSuchBuket extends TestTramLiveDataConfig {
            @Override
            public String getS3Bucket() {
                return "NoSuckBucketShouldExist";
            }
        }
    }
}
