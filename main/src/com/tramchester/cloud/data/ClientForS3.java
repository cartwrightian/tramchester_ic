package com.tramchester.cloud.data;

import com.google.common.collect.Streams;
import com.google.common.io.ByteStreams;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.Zipper;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.URLStatus;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;

@LazySingleton
public class ClientForS3 {
    public static final String ORIG_MOD_TIME_META_DATA_KEY = "original_mod_time";
    private static final Logger logger = LoggerFactory.getLogger(ClientForS3.class);

    private final GetsFileModTime getsFileModTime;
    private final Zipper zipper;
    protected S3Client s3Client;
    private MessageDigest messageDigest;

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Inject
    public ClientForS3(GetsFileModTime getsFileModTime, Zipper zipper) {
        this.getsFileModTime = getsFileModTime;
        this.zipper = zipper;
        s3Client = null;
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            s3Client = S3Client.create();
            logger.info("Started");
        } catch (AwsServiceException | SdkClientException exception) {
            logger.error("Unable to init S3 client", exception);
        } catch (NoSuchAlgorithmException exception) {
            logger.error("Unable to file algo for message digest");
        }
    }

    @PreDestroy
    public void stop() {
        if (s3Client != null) {
            logger.info("Stopping");
            s3Client.close();
            s3Client = null;
        } else {
            logger.warn("Was not started");
        }
    }

    public boolean upload(final String bucket, final String key, final Path fileToUpload) {
        logger.info(format("Uploading to bucket '%s' key '%s' file '%s'", bucket, key, fileToUpload.toAbsolutePath()));
        if (!isStarted()) {
            logger.error("Not started");
            return false;
        }

        final ZonedDateTime fileModTime = getsFileModTime.getFor(fileToUpload);

        try {
            final byte[] buffer = Files.readAllBytes(fileToUpload);
            final String localMd5 = Base64.encodeBase64String(messageDigest.digest(buffer));
            return uploadToS3(bucket, key, localMd5, RequestBody.fromBytes(buffer), fileModTime);

        } catch (IOException e) {
           logger.info("Unable to upload file " + fileToUpload.toAbsolutePath(), e);
           return false;
        }
    }

    public boolean uploadZipped(final String bucket, final String key, final Path original) {
        logger.info(format("Zip and Uploading to bucket '%s' key '%s' file '%s'", bucket, key, original.toAbsolutePath()));

        try {
            final ByteArrayOutputStream outputStream = zipper.zip(original);
            final byte[] buffer = outputStream.toByteArray();
            final String localMd5 = Base64.encodeBase64String(messageDigest.digest(buffer));
            final ZonedDateTime fileModTime = getsFileModTime.getFor(original);

            // todo ideally pass in a stream to avoid having whole file in memory, but no way to do local MD5
            // if that is done.....
            return uploadToS3(bucket, key, localMd5, RequestBody.fromBytes(buffer), fileModTime);
        } catch (IOException e) {
            logger.error("Unable to upload (zipped) file " + original.toAbsolutePath(), e);
            return false;
        }
    }

    public boolean upload(String bucket, String key, String text, ZonedDateTime modTimeMetaData) {
        if (!isStarted()) {
            logger.error("not started");
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(format("Uploading to bucket '%s' key '%s' contents '%s'", bucket, key, text));
        } else {
            logger.info(format("Uploading to bucket '%s' key '%s'", bucket, key));
        }

        if (!bucketExists(bucket)) {
            logger.error(format("Bucket %s does not exist", bucket));
            return false;
        }

        byte[] bytes = text.getBytes();
        String localMd5 = Base64.encodeBase64String(messageDigest.digest(bytes));
        final RequestBody requestBody = RequestBody.fromBytes(bytes);

        return uploadToS3(bucket, key, localMd5, requestBody, modTimeMetaData);
    }

    private boolean uploadToS3(final String bucket, final String key, final String localMd5, final RequestBody requestBody,
                               final ZonedDateTime fileModTime) {
        if (!isStarted()) {
            logger.error("No started, uploadToS3");
            return false;
        }

        final Map<String, String> metaData = new HashMap<>();
        final String modTimeText = fileModTime.format(DATE_TIME_FORMATTER);
        logger.info(format("Set %s to %s", ORIG_MOD_TIME_META_DATA_KEY, modTimeText));
        metaData.put(ORIG_MOD_TIME_META_DATA_KEY, modTimeText);

        try {
            logger.debug("Uploading with MD5: " + localMd5);
            final PutObjectRequest putObjectRequest = PutObjectRequest.builder().
                    bucket(bucket).
                    metadata(metaData).
                    key(key).
                    build();
            s3Client.putObject(putObjectRequest, requestBody);
        } catch (AwsServiceException awsServiceException) {
            logger.error(format("AWS exception during upload for upload to bucket '%s' key '%s'", bucket, key), awsServiceException);
            return false;
        }
        return true;
    }

    public <T> List<T> downloadAndMapForKey(String bucket, String key, LiveDataClientForS3.ResponseMapper<T> responseMapper) {

        GetObjectRequest request = GetObjectRequest.builder().
                bucket(bucket).key(key).build();

        ResponseTransformer<GetObjectResponse, List<T>> transformer =
                (response, inputStream) -> responseMapper.map(key, readBytes(bucket, key, response, inputStream));

        return s3Client.getObject(request, transformer);
    }

    protected byte[] readBytes(String bucket, String key, GetObjectResponse response, FilterInputStream inputStream) {
        int contentLength = Math.toIntExact(response.contentLength());

        logger.debug(format("Key: %s Content type: %s Length %s ", key, response.contentType(), contentLength));
        byte[] bytes = new byte[contentLength];
        int offset = 0;
        int read;
        try {
            do {
                read = inputStream.read(bytes, offset, contentLength - offset);
                offset = offset + read;
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Key %s Read %s of %s bytes", key, read, contentLength));
                }
            } while (read > 0);
            inputStream.close();

            checkMD5(bucket, key, response, bytes);

        } catch (IOException exception) {
            logger.error("Exception downloading from bucket " + bucket + " with key " + key, exception);
        }
        return bytes;
    }

    private void checkMD5(String bucket, String key, GetObjectResponse response, byte[] bytes) {
        String remote = getETagClean(response);
        String localMd5 = DigestUtils.md5Hex(bytes);
        if (!localMd5.equals(remote)) {
            logger.error(format("MD5 mismatch downloading from bucket %s key %s local '%s' remote '%s'",
                    bucket, key, localMd5, remote));
        } else if (logger.isDebugEnabled()) {
            logger.debug(format("MD5 match for %s key %s md5: '%s'", bucket, key, localMd5));
        }
    }

    private String getETagClean(GetObjectResponse response) {
        String remote = response.eTag();
        if (remote.startsWith("\"")) {
            remote = remote.replaceFirst("\"", "");
        }
        if (remote.endsWith("\"")) {
            remote = remote.substring(0, remote.length() - 1);
        }
        return remote;
    }

    // NOTE: listing all buckets requires permissions beyond just the one bucket,
    // so here do an op on the bucket and catch exception instead
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean bucketExists(final String bucket) {
        if (!isStarted()) {
            logger.error("not started, bucket exists");
            return false;
        }

        if (bucket.isEmpty()) {
            throw new RuntimeException("Bucket name cannot be empty string");
        }

        try {
            GetBucketLocationRequest request = GetBucketLocationRequest.builder().bucket(bucket).build();
            s3Client.getBucketLocation(request);
        } catch (AwsServiceException exception) {
            if (exception.awsErrorDetails().errorCode().equals("NoSuchBucket")) {
                logger.info("Bucket " + bucket + " not found");
            } else {
                logger.error("Could not check for existence of bucket " + bucket, exception);
            }
            return false;
        }
        return true;
    }

    public boolean keyExists(String bucket, String prefix, String itemName) {
        if (!isStarted()) {
            logger.error("not started, keyExists");
            return false;
        }

        String fullKey = prefix + "/" + itemName;

        Stream<S3Object> items = getSummaryForPrefix(bucket, prefix);

        Optional<S3Object> search = items.filter(item -> fullKey.equals(item.key())).findAny();

        if (search.isPresent()) {
            logger.info(format("Key %s is present in bucket %s", fullKey, bucket));
            return true;
        }

        logger.info(format("Key %s is not present in bucket %s", fullKey, bucket));

        return false;
    }

    public Stream<String> getKeysFor(String bucket, String prefix) {
        if (!isStarted()) {
            logger.error("not started, getKeysFor");
            return Stream.empty();
        }

        return getSummaryForPrefix(bucket, prefix).map(S3Object::key);
    }

    /***
     * Warning: performance on large buckets
     * @param bucket location to use
     * @return a stream of all keys in the bucket
     */
    public Stream<String> getAllKeys(String bucket) {
        if (!isStarted()) {
            logger.error("Not started, getSummaryForPrefix");
            return Stream.empty();
        }

        if (!bucketExists(bucket)) {
            logger.error(format("Bucket %s does not exist so cannot get summary", bucket));
            return Stream.empty();
        }

        logger.info("Get key stream for bucket " + bucket);

        return Streams.stream(new KeyIterator(s3Client, bucket)).map(S3Object::key);
    }

    public Stream<S3Object> getSummaryForPrefix(String bucket, String prefix) {
        if (!isStarted()) {
            logger.error("Not started, getSummaryForPrefix");
            return Stream.empty();
        }

        if (!bucketExists(bucket)) {
            logger.error(format("Bucket %s does not exist so cannot get summary", bucket));
            return Stream.empty();
        }

        logger.info("Get stream for bucket " + bucket + " prefix " + prefix);
        return Streams.stream(new KeyIterator(s3Client, bucket, prefix));
    }

    public boolean isStarted() {
        return s3Client != null;
    }

    public ZonedDateTime getModTimeFor(URI uri) throws FileNotFoundException {
        logger.info("Fetch Mod time for url " + uri);
        BucketKey bucketKey = BucketKey.convertFromURI(uri);

        if (!isStarted()) {
            logger.error("Not started");
            return URLStatus.invalidTime;
        }

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().key(bucketKey.key).bucket(bucketKey.bucket).build();
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);

            ResponseFacade responseFacade = getResponseFacade(response);
            return overrideModTimeIfMetaData(responseFacade, uri);
        }
        catch(NoSuchKeyException noSuchKeyException) {
            logger.warn("Could not get object for missing uri " + uri + " and key " + bucketKey);
            throw new FileNotFoundException(uri.toString());
        }
        catch(SdkClientException clientException) {
            logger.error("Unable to communicate with S3", clientException);
            return URLStatus.invalidTime;
        }
    }


    public boolean deleteKeys(final String bucket, final List<S3Object> items) {
        logger.warn("Request to delete " + items.size() + " keys");

        final List<ObjectIdentifier> itemIds = items.stream().map(this::createObjectId).toList();

        final Delete toDelete = Delete.builder().objects(itemIds).build();

        final DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder().
                bucket(bucket).delete(toDelete).build();

        final DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);

        if (response.hasDeleted()) {
            final List<DeletedObject> deletedIds =  response.deleted();
            logger.info("Deleted " + deletedIds.size());
            deletedIds.forEach(deletedObject -> logger.info("Deleted " + deletedObject.key()));
        } else {
            logger.warn("Deleted no objects");
        }

        boolean hasErrors = response.hasErrors();
        if (hasErrors) {
            List<S3Error> errors = response.errors();
            logger.error("Got " + errors.size() + " errors during delete.");
            errors.forEach(error -> logger.error(String.format("Failed deleted for %s %s %s",error.key(), error.code(), error.message())));
        }
        return !hasErrors;
    }

    private ObjectIdentifier createObjectId(S3Object item) {
        return ObjectIdentifier.builder().
                key(item.key()).
                build();
    }

    private ZonedDateTime overrideModTimeIfMetaData(final ResponseFacade response, final URI uri) {
        if (response.hasMetadata()) {
            final Map<String, String> meta = response.metadata();
            if (meta.containsKey(ORIG_MOD_TIME_META_DATA_KEY)) {
                final String modTimeTxt = meta.get(ORIG_MOD_TIME_META_DATA_KEY);
                logger.info("Metadata " + ORIG_MOD_TIME_META_DATA_KEY + " present with value '" + modTimeTxt + "' so using in preference to native S3 mod time for " + uri);
                return parseMetaDataDateTime(modTimeTxt);
            }
        }

        logger.warn("No " + ORIG_MOD_TIME_META_DATA_KEY + " is present, fall back to native S3 mod time for " + uri);
        Instant lastModified = response.lastModified();
        return ZonedDateTime.ofInstant(lastModified, UTC);
    }

    private static @NotNull ZonedDateTime parseMetaDataDateTime(String modTimeTxt) {
        try {
            return ZonedDateTime.parse(modTimeTxt, DATE_TIME_FORMATTER);
        }
        catch (DateTimeParseException exception) {
            logger.error("Unable to parse '" + modTimeTxt + "' so attempt fall back to previous format");
            final LocalDateTime localTime = LocalDateTime.parse(modTimeTxt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            final ZonedDateTime zonedDateTime = localTime.atZone(TramchesterConfig.TimeZoneId).withZoneSameInstant(UTC);
            logger.warn("Fall back to local time for '" + modTimeTxt + "' with local " + localTime + " result " + zonedDateTime);
            return zonedDateTime;
        }
    }

    private GetObjectRequest createRequestFor(BucketKey bucketKey) {
        logger.info("Create getRequest for " + bucketKey);
        return GetObjectRequest.builder().
                bucket(bucketKey.bucket).key(bucketKey.key).
                build();
    }

    public URLStatus downloadTo(final Path path, final URI uri) throws IOException {
        logger.info("Download for for url " + uri);

        if (!isStarted()) {
            String msg = "Not started, downloadTo";
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        final BucketKey bucketKey = BucketKey.convertFromURI(uri);

        if (!bucketKey.isValid()) {
            String msg = "Bucket provided in URI is not valid " + uri;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        GetObjectRequest getObjectRequest = createRequestFor(bucketKey);

        final ResponseInputStream<GetObjectResponse> responseInputStream;
        try {
            responseInputStream = s3Client.getObject(getObjectRequest);
        }
        catch (SdkClientException sdkClientException) {
            logger.error("Failure to connect to S3", sdkClientException);
            return URLStatus.NetworkError(uri);
        }

        final GetObjectResponse response = responseInputStream.response();
        final String remoteMD5 = getETagClean(response);

        ResponseFacade responseFacade = createResponseFacade(response);

        final ZonedDateTime modTime = overrideModTimeIfMetaData(responseFacade, uri);

        File file = path.toFile();
        FileOutputStream output = new FileOutputStream(file);
        ByteStreams.copy(responseInputStream, output);
        responseInputStream.close();
        output.close();

        logger.info(format("Update downloaded file %s modtime to %s", path.toAbsolutePath(), modTime));
        getsFileModTime.update(path, modTime);

        InputStream writtenFile = new FileInputStream(file);
        final String localMd5 = DigestUtils.md5Hex(writtenFile);
        writtenFile.close();

        if (!localMd5.equals(remoteMD5)) {
            logger.warn(format("MD5 mismatch downloading from %s local '%s' remote '%s'",
                    bucketKey, localMd5, remoteMD5));
        } else  {
            logger.info(format("Downloaded to %s from %s MD5 match md5: '%s'", path.toAbsolutePath(), bucketKey, localMd5));
        }

        return new URLStatus(uri, 200, modTime);
    }


    private record BucketKey(String bucket, String key) {

        private static BucketKey convertFromURI(URI uri) {
                String scheme = uri.getScheme();
                if (!"s3".equals(scheme)) {
                    throw new RuntimeException("s3 only, got " + scheme);
                }
                String bucket = uri.getHost();
                String key = uri.getPath().replaceFirst("/", "");
                return new BucketKey(bucket, key);
            }

            @Override
            public String toString() {
                return "BucketPrefixKey{" +
                        "bucket='" + bucket + '\'' +
                        ", key='" + key + '\'' +
                        '}';
            }

            public boolean isValid() {
                return !(bucket.isEmpty() || key.isEmpty());
            }
        }

    private interface ResponseFacade {
        boolean hasMetadata();
        Map<String, String> metadata();
        Instant lastModified();
    }

    @NotNull
    private static ResponseFacade createResponseFacade(GetObjectResponse response) {
        return new ResponseFacade() {
            @Override
            public boolean hasMetadata() {
                return response.hasMetadata();
            }

            @Override
            public Map<String, String> metadata() {
                return response.metadata();
            }

            @Override
            public Instant lastModified() {
                return response.lastModified();
            }
        };
    }

    @NotNull
    private static ResponseFacade getResponseFacade(HeadObjectResponse response) {
        return new ResponseFacade() {
            @Override
            public boolean hasMetadata() {
                return response.hasMetadata();
            }

            @Override
            public Map<String, String> metadata() {
                return response.metadata();
            }

            @Override
            public Instant lastModified() {
                return response.lastModified();
            }
        };
    }

    private static class KeyIterator implements Iterator<S3Object> {
        private static final Logger logger = LoggerFactory.getLogger(KeyIterator.class);

        private final S3Client s3Client;
        private final String bucket;
        private final LinkedList<S3Object> buffer;
        private final ListObjectsV2Request.Builder builder;
        private boolean moreRemaining;

        public KeyIterator(S3Client s3Client, String bucket) {
            this(s3Client, bucket, null);
        }

        public KeyIterator(S3Client s3Client, String bucket, String prefix) {
            this.s3Client = s3Client;
            this.bucket = bucket;
            buffer = new LinkedList<>();
            builder = ListObjectsV2Request.builder();
            moreRemaining = true;

            builder.bucket(bucket);
            if (prefix!=null) {
                builder.prefix(prefix);
            }
        }

        @Override
        public boolean hasNext() {
            if (moreInBuffer()) {
                return true;
            }

            if (!moreRemaining) {
                return false;
            }

            try {
                final ListObjectsV2Request listObsRequest = builder.build();
                final ListObjectsV2Response response = s3Client.listObjectsV2(listObsRequest);
                final List<S3Object> contents = response.contents();
                logger.info("Fetched " + contents.size()+ " records");
                buffer.addAll(contents);
                final String continueToken = response.nextContinuationToken();
                builder.continuationToken(continueToken);
                moreRemaining = response.isTruncated();
            } catch (S3Exception exception) {
                String msg = format("Cannot get objects for in bucket '%s' reason '%s'",
                        bucket, exception.getMessage());
                logger.warn(msg, exception);
                throw new RuntimeException(msg);
            }

            return moreInBuffer() || moreRemaining;
        }

        private boolean moreInBuffer() {
            return !buffer.isEmpty();
        }

        @Override
        public S3Object next() {
            if (buffer.isEmpty()) {
                throw new RuntimeException("Unexpected call to next() when buffer is empty");
            }
            return buffer.removeFirst();
        }
    }
}
