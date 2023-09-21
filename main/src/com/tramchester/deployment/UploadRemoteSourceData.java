package com.tramchester.deployment;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class UploadRemoteSourceData {
    private static final Logger logger = LoggerFactory.getLogger(UploadRemoteSourceData.class);

    private final UploadFileToS3 uploadFileToS3;
    private final TramchesterConfig config;
    private final RemoteDataAvailable remoteDataRefreshed;

    @Inject
    public UploadRemoteSourceData(UploadFileToS3 uploadFileToS3, TramchesterConfig config,
                                  RemoteDataAvailable remoteDataRefreshed) {
        this.uploadFileToS3 = uploadFileToS3;
        this.config = config;
        this.remoteDataRefreshed = remoteDataRefreshed;
    }

    public boolean upload(String prefixForS3Key) {
        logger.info("Upload data sources to " + prefixForS3Key);
        List<RemoteDataSourceConfig> remoteSources = config.getRemoteDataSourceConfig();
        List<DataSourceID> remoteWithFiles = remoteSources.stream().
                map(RemoteDataSourceConfig::getDataSourceId).
                filter(remoteDataRefreshed::hasFileFor).
                toList();
        if (remoteWithFiles.isEmpty()) {
            logger.error("No remote sources had files");
        } else {
            logger.info("Uploading " + remoteWithFiles);
        }

        return remoteWithFiles.stream().allMatch(dataSourceId -> upload(prefixForS3Key, dataSourceId));
    }

    private boolean upload(String prefixForS3Key, DataSourceID dataSourceId) {

        final Path path = remoteDataRefreshed.fileFor(dataSourceId);

        logger.info(format("Upload data source %s for %s and key '%s'", dataSourceId, path, prefixForS3Key));

        String filename = path.getFileName().toString();

        boolean result;
        if (filename.toLowerCase().endsWith(".xml")) {
            result = uploadFileToS3.uploadFileZipped(prefixForS3Key, path, true);
        } else {
            result = uploadFileToS3.uploadFile(prefixForS3Key, path, true);
        }

        if (!result) {
            logger.error("Unable to upload for " + dataSourceId + " check above logs for failures");
        }
        return result;
    }
}
