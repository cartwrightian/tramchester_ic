package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.ClientForS3;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

@LazySingleton
public class S3DownloadAndModTime implements DownloadAndModTime {
    private static final Logger logger = LoggerFactory.getLogger(S3DownloadAndModTime.class);

    private final ClientForS3 s3Client;

    @Inject
    public S3DownloadAndModTime(ClientForS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public URLStatus getStatusFor(URI url, ZonedDateTime localModTime, boolean warnIfMissing, List<Pair<String, String>> headers) {

        if (!headers.isEmpty()) {
            throw new RuntimeException("Headers are not supported for S3 download " + headers);
        }

        try {
            return new URLStatus(url, 200, s3Client.getModTimeFor(url));
        } catch (FileNotFoundException notFoundException) {
            if (warnIfMissing) {
                logger.warn("Missing resource "+ url, notFoundException);
            } else {
                logger.info("Did not find " + url);
            }
            return new URLStatus(url, 404);
        }
    }

    @Override
    public URLStatus downloadTo(Path path, URI url, ZonedDateTime localModTime, List<Pair<String, String>> headers) throws IOException {
        if (!headers.isEmpty()) {
            throw new RuntimeException("Headers are not supported for S3 download " + headers);
        }
        // there is no equivalent to if-modified-since so don't use localModTime here
        return s3Client.downloadTo(path, url);
    }

}
