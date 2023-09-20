package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.ClientForS3;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;

@LazySingleton
public class S3DownloadAndModTime implements DownloadAndModTime {
    private static final Logger logger = LoggerFactory.getLogger(S3DownloadAndModTime.class);

    private final ClientForS3 s3Client;

    @Inject
    public S3DownloadAndModTime(ClientForS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public URLStatus getStatusFor(URI url, LocalDateTime localModTime) {
        try {
            return new URLStatus(url, HttpStatus.SC_OK, s3Client.getModTimeFor(url));
        } catch (FileNotFoundException notFoundException) {
            logger.warn("Missing resource ", notFoundException);
            return new URLStatus(url, HttpStatus.SC_NOT_FOUND);
        }
    }

    @Override
    public URLStatus downloadTo(Path path, URI url, LocalDateTime localModTime) throws IOException {
        // there is no equivalent to if-modified-since so don't use localModTime here
        return s3Client.downloadTo(path, url);
    }

}
