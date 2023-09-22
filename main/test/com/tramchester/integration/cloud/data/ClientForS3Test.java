package com.tramchester.integration.cloud.data;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.S3Test;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@S3Test
public class ClientForS3Test {
    private static S3Client awsS3;
    private static S3Waiter s3Waiter;
    private static S3TestSupport s3TestSupport;

    private static GuiceContainerDependencies componentContainer;

    // TOOD Use a different bucket from the live data one
    private final static String BUCKET = "tramchestertestlivedatabucketnew";
    private static ClientForS3 clientForS3;
    private final Path testFilePath = Path.of("testFile.txt");

    @BeforeAll
    static void beforeAnyDone() {

        TramchesterConfig configuration = new TestBucketConfig(BUCKET);
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        awsS3 = S3Client.builder().build();
        s3Waiter = S3Waiter.create();

        s3TestSupport = new S3TestSupport(awsS3, s3Waiter, configuration.getDistributionBucket());
        s3TestSupport.createOrCleanBucket();

        clientForS3 = componentContainer.get(ClientForS3.class);
    }

    @AfterAll
    static void afterAllDone() {
        componentContainer.close();

        //s3TestSupport.deleteBucket();
        s3Waiter.close();
        awsS3.close();
    }

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
        s3TestSupport.cleanBucket();
    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
        s3TestSupport.cleanBucket();
    }

    @Test
    void shouldCreateThenDeleteAKey() throws IOException {

        final String fullKey = "test/key";
        final String text = "someTextToPlaceInS3";
        clientForS3.upload(BUCKET, fullKey, text, TestEnv.LocalNow());

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // exists?
        assertTrue(clientForS3.keyExists(BUCKET, "test", "key"));

        // get contents
        String resultText = getContentsOfKey(fullKey);
        assertEquals(text, resultText);

        // delete
        awsS3.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET).key(fullKey).build());
        s3Waiter.waitUntilObjectNotExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // not exists?
        assertFalse(clientForS3.keyExists(BUCKET, "test", "key"));
    }

    @Test
    void shouldUploadFromFile() throws IOException, URISyntaxException {

        LocalDateTime originalModTime = LocalDateTime.of(1984, 10, 20, 21, 58, 59);

        final String text = "someTextInAFileToUploadToS3";
        Files.writeString(testFilePath, text);

        final String fullKey = "test/file";

        testFilePath.toFile().setLastModified(originalModTime.atZone(TramchesterConfig.TimeZoneId).toInstant().toEpochMilli());

        clientForS3.upload(BUCKET, fullKey, testFilePath);

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // exists
        assertTrue(clientForS3.keyExists(BUCKET, "test", "file"));

        HeadObjectRequest headRequest = HeadObjectRequest.builder().key(fullKey).bucket(BUCKET).build();
        HeadObjectResponse response = awsS3.headObject(headRequest);

        assertTrue(response.hasMetadata());
        String modTimeAsTxt = response.metadata().get(ClientForS3.ORIG_MOD_TIME_META_DATA_KEY);
        assertEquals(originalModTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), modTimeAsTxt);

        // get contents
        String resultText = getContentsOfKey(fullKey);
        assertEquals(text, resultText);

        String urlText = format("s3://%s/%s", BUCKET, fullKey);
        LocalDateTime modTimeFromClient = clientForS3.getModTimeFor(new URI(urlText));
        assertEquals(originalModTime, modTimeFromClient);

        // delete
        awsS3.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET).key(fullKey).build());
        s3Waiter.waitUntilObjectNotExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // not exists?
        assertFalse(clientForS3.keyExists(BUCKET, "test", "key"));
    }

    @Test
    void shouldGetKeysForBucketWithPrefix() {

        Stream<String> streamA = clientForS3.getKeysFor(BUCKET, "test");
        Set<String> resultA = streamA.collect(Collectors.toSet());
        assertTrue(resultA.isEmpty());

        String keyA = "test/keyA";
        String keyB = "test/keyB";
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(keyA).build(), RequestBody.fromString("text1"));
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(keyB).build(), RequestBody.fromString("text2"));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(keyA).build());
        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(keyB).build());

        Stream<String> streamB = clientForS3.getKeysFor(BUCKET, "test");
        Set<String> resultB = streamB.collect(Collectors.toSet());
        assertEquals(2, resultB.size());

        assertTrue(resultB.contains(keyA));
        assertTrue(resultB.contains(keyB));
    }

    @Test
    void shouldGetAllKeysForBucket() {

        Stream<String> streamA = clientForS3.getAllKeys(BUCKET);
        Set<String> resultA = streamA.collect(Collectors.toSet());
        assertTrue(resultA.isEmpty());

        String keyA = "test/keyA";
        String keyB = "test/keyB";
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(keyA).build(), RequestBody.fromString("text1"));
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(keyB).build(), RequestBody.fromString("text2"));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(keyA).build());
        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(keyB).build());

        Stream<String> streamB = clientForS3.getKeysFor(BUCKET, "test");
        Set<String> resultB = streamB.collect(Collectors.toSet());
        assertEquals(2, resultB.size());

        assertTrue(resultB.contains(keyA));
        assertTrue(resultB.contains(keyB));
    }

    @Test
    void shouldGetModTime() throws IOException {
        String key = "test/keyModTime";

        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(key).build(), RequestBody.fromString("text1"));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        ResponseInputStream<GetObjectResponse> stream = awsS3.getObject(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build());
        Instant instantFromS3 = stream.response().lastModified();
        ZonedDateTime dateTimeFromS3 = instantFromS3.atZone(TramchesterConfig.TimeZoneId);
        stream.close();

        String urlText = format("s3://%s/%s", BUCKET, key);
        LocalDateTime modTime = clientForS3.getModTimeFor(URI.create(urlText));

        assertEquals(dateTimeFromS3.toLocalDateTime(), modTime);
    }

    @Test
    void shouldGetModTimeMetaDataOverride() throws IOException {
        String key = "test/keyModTime";

        LocalDateTime originalModTime = LocalDateTime.of(1984, 10, 20, 21, 58, 59);
        String originalModTimeText = originalModTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, String> meta = new HashMap<>();
        meta.put(ClientForS3.ORIG_MOD_TIME_META_DATA_KEY, originalModTimeText);
        PutObjectRequest putReq = PutObjectRequest.builder().
                bucket(BUCKET).key(key).
                metadata(meta).
                build();

        awsS3.putObject(putReq, RequestBody.fromString("text1"));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        String urlText = format("s3://%s/%s", BUCKET, key);
        LocalDateTime modTime = clientForS3.getModTimeFor(URI.create(urlText));

        assertEquals(originalModTime, modTime);
    }


    @Test
    void shouldThrowForMissingKey() {
        String key = "SHOULDNOTEXIST";
        String urlText = format("s3://%s/%s", BUCKET, key);
        FileNotFoundException expected = assertThrows(FileNotFoundException.class, () -> clientForS3.getModTimeFor(URI.create(urlText)));

        assertEquals(urlText, expected.getMessage());
    }

    @Test
    void shouldDownloadAndMapSet() {
        String key = "test/keyDownloadAndMap";

        final String text = "testToUpdateForMapper";
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(key).build(), RequestBody.fromString(text));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        List<String> result = clientForS3.downloadAndMapForKey(BUCKET, key, (receivedKey, bytes) -> Collections.singletonList(new String(bytes)));

        assertEquals(1, result.size());
        assertTrue(result.contains(text));
    }

    @Test
    void shouldDownloadToFile() throws IOException {

        LocalDateTime originalModTime = LocalDateTime.of(1984, 10, 20, 21, 58, 59);

        String key = "test/keyDownloadAndSave";

        final String text = "textToDownloadToAFile";
        Map<String, String> metaData = new HashMap<>();

        metaData.put(ClientForS3.ORIG_MOD_TIME_META_DATA_KEY, originalModTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).metadata(metaData).key(key).build(), RequestBody.fromString(text));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        String textURL = format("s3://%s/%s", BUCKET, key);
        clientForS3.downloadTo(testFilePath, URI.create(textURL));

        assertTrue(Files.exists(testFilePath));

        byte[] bytes = Files.readAllBytes(testFilePath);
        String result = new String(bytes);
        assertEquals(text, result);

        long lastModMills = testFilePath.toFile().lastModified();
        LocalDateTime lastModLocal = Instant.ofEpochMilli(lastModMills).atZone(TramchesterConfig.TimeZoneId).toLocalDateTime();
        assertEquals(originalModTime, lastModLocal.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void shouldUploadWithZip() throws IOException {
        String key = "test/keyZipAndUpload";

        final String text = "someTextToUploadThatShouldEndUpZipped";
        Files.writeString(testFilePath, text);

        boolean ok = clientForS3.uploadZipped(BUCKET, key, testFilePath);
        assertTrue(ok);

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        ResponseInputStream<GetObjectResponse> result = awsS3.getObject(GetObjectRequest.builder().
                bucket(BUCKET).
                key(key).build());
        byte[] bytes = result.readAllBytes();

        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));

        ZipEntry entry = zipInputStream.getNextEntry();
        assertNotNull(entry);
        assertEquals(testFilePath.getFileName().toString(), entry.getName());

        byte[] outputBuffer = new byte[text.length()];
        int read = zipInputStream.read(outputBuffer, 0, text.length());
        zipInputStream.closeEntry();
        zipInputStream.close();

        assertEquals(text.length(), read);
        assertEquals(text, new String(outputBuffer));


    }

    @NotNull
    private String getContentsOfKey(String key) throws IOException {
        ResponseInputStream<GetObjectResponse> result = awsS3.getObject(GetObjectRequest.builder().
                bucket(BUCKET).
                key(key).build());
        byte[] bytes = result.readAllBytes();
        return new String(bytes);
    }

    public static class TestBucketConfig extends TestConfig {

        private final String bucketName;

        public TestBucketConfig(String bucketName) {
            this.bucketName = bucketName;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public String getDistributionBucket() {
            return bucketName;
        }

    }

}
