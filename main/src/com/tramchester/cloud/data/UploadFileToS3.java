package com.tramchester.cloud.data;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;

import static java.lang.String.format;

@LazySingleton
public class UploadFileToS3 {
    private static final Logger logger = LoggerFactory.getLogger(UploadFileToS3.class);

    private final ClientForS3 clientForS3;
    private final String bucket;

    @Inject
    public UploadFileToS3(ClientForS3 clientForS3, TramchesterConfig config) {
        this.clientForS3 = clientForS3;
        this.bucket = config.getDistributionBucket();
    }

    /***
     * Upload a file to S3
     * @param prefixForKey where to place the item
     * @param fileToUpload the item to upload
     * @param overWrite over-write the item if already present
     * @return true if file uploads ok, false otherwise
     */
    public boolean uploadFile(String prefixForKey, Path fileToUpload, boolean overWrite) {
        guardStarted();

        logger.info(format("Upload %s to %s overwrite:%s", fileToUpload, prefixForKey, overWrite));

        String itemId = fileToUpload.getFileName().toString();

        if (!overWrite) {
            if (clientForS3.keyExists(bucket, prefixForKey, itemId)) {
                logger.error(format("prefix %s key %s already exists", prefixForKey, itemId));
                return false;
            }
        }

        final String key = prefixForKey + "/" + itemId;
        logger.info(format("Upload file %s to bucket %s at %s", fileToUpload.toAbsolutePath(), bucket, key));
        return clientForS3.upload(bucket, key, fileToUpload);
    }

    /***
     * Upload a file to S3 after zipping
     * @param prefixForKey where to place the item
     * @param originalFile the item to upload
     * @param overwrite over-write the item if already present
     * @return true if file uploads ok, false otherwise
     */
    public boolean uploadFileZipped(String prefixForKey, Path originalFile, boolean overwrite, String key) {
        guardStarted();

        logger.info(format("Upload zipped %s to %s overwrite:%s", originalFile, prefixForKey, overwrite));

        if (!overwrite) {
            if (clientForS3.keyExists(bucket, prefixForKey, key)) {
                logger.error(format("prefix %s key %s already exists", prefixForKey, key));
                return false;
            }
        }

        final String keyForZipped = prefixForKey + "/" + key;
        logger.info(format("Upload file %s zipped to bucket %s at %s", originalFile.toAbsolutePath(), bucket, keyForZipped));

        return clientForS3.uploadZipped(bucket, keyForZipped, originalFile);
    }

    private void guardStarted() {
        if (!clientForS3.isStarted()) {
            throw new RuntimeException("S3 client is not started");
        }
    }


}
