package com.tramchester.cloud.data;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@LazySingleton
public class LiveDataClientForS3  {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataClientForS3.class);
    private final ClientForS3 clientForS3;
    private final String bucket;
    private boolean started;

    @Inject
    public LiveDataClientForS3(TramchesterConfig config, ClientForS3 clientForS3) {
        TfgmTramLiveDataConfig liveDataConfig = config.getLiveDataConfig();
        if (liveDataConfig!=null) {
            bucket = liveDataConfig.getS3Bucket();
        } else {
            bucket = "";
        }
        this.clientForS3 = clientForS3;
        started = false;
    }

    @PostConstruct
    public void start() {
        if (bucket.isEmpty()) {
            logger.info("Not starting, no live data bucket config");
        } else {
            started = true;
            logger.info("Started");
        }
    }

    @PreDestroy
    public void stop() {
        if (!started) {
            return;
        }
        logger.info("Stopped");
    }

    private void guardForStarted() {
        if (!started) {
            String message = "Not started, no config for Live Data S3 upload set?";
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    /***
     * @param keys set of keys to download
     * @param responseMapper mapper function to apply to the resulting s3objects
     * @param <T> return type for the mapper and hence resulting stream
     * @return retreieved s3objects with the mappng applied
     */
    public <T> Stream<T> downloadAndMap(final Set<String> keys, ResponseMapper<T> responseMapper) {
        guardForStarted();
        logger.info("Downloading data and map for " + keys.size() + " keys");
        return downloadAndMap(keys.stream(), responseMapper);
    }

    public <T> Stream<T> downloadAndMap(final Stream<String> keys, ResponseMapper<T> responseMapper) {
        guardForStarted();

//        if (bucket.isEmpty()) {
//            logger.error("not started");
//            return Stream.empty();
//        }

        logger.info("Downloading keys for bucket " + bucket);
        Stream<T> stream = keys.map(key -> clientForS3.downloadAndMapForKey(bucket, key, responseMapper)).flatMap(Collection::stream);
        logger.info("Return stream");
        return stream;
    }

    public boolean isEnabled() {
        return started;
    }

    public boolean itemExists(String prefix, String item) {
        guardForStarted();
        logger.info(String.format("Check for prefix '%s' item '%s'", prefix, item));
        return clientForS3.keyExists(bucket, prefix, item);
    }

    public boolean upload(String key, String json, ZonedDateTime modTime) {
        guardForStarted();
        return clientForS3.upload(bucket, key, json, modTime);
    }

    public Stream<String> getKeysFor(String prefix) {
        guardForStarted();
        return clientForS3.getKeysFor(bucket, prefix);
    }

    public interface ResponseMapper<T> {
        List<T> map(final String key, final byte[] bytes);
    }

}
