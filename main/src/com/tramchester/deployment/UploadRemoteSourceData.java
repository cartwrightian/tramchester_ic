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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        final List<RemoteDataSourceConfig> remoteSources = config.getRemoteDataSourceConfig();

        Set<DataSourceID> toSkip = remoteSources.stream().
                filter(RemoteDataSourceConfig::getSkipUpload).
                map(RemoteDataSourceConfig::getDataSourceId).
                collect(Collectors.toSet());

        if (!toSkip.isEmpty()) {
            logger.info("Will skip uploading following sources " + toSkip);
        }

        List<DataSourceID> remoteWithFiles = new ArrayList<>(remoteSources.stream().
                map(RemoteDataSourceConfig::getDataSourceId).
                filter(id -> !toSkip.contains(id)).
                filter(remoteDataRefreshed::hasFileFor).
                toList());

        if (remoteWithFiles.isEmpty()) {
            logger.error("No remote sources had files");
        } else {
            logger.info("Uploading " + remoteWithFiles);
        }

        return remoteWithFiles.stream().allMatch(dataSourceId -> upload(prefixForS3Key, config.getDataRemoteSourceConfig(dataSourceId)));
    }

    private boolean upload(String prefixForS3Key, RemoteDataSourceConfig dataSourceConfig) {

        DataSourceID dataSourceId = dataSourceConfig.getDataSourceId();

        //logger.info(format("Upload data for %s to S3 prefix %s", dataSourceId, prefixForS3Key));

        Path localPath;
        if (dataSourceConfig.hasModCheckFilename()) {
            localPath = dataSourceConfig.getDataPath().resolve(dataSourceConfig.getModTimeCheckFilename());
            logger.info(format("Data source %s Mod check file name is present, will use %s as source, prefix S3 %s",
                    dataSourceId, localPath, prefixForS3Key));
        } else {
            localPath = dataSourceConfig.getDownloadPath().resolve(dataSourceConfig.getDownloadFilename());
            logger.info(format("Data source %s will use %s as source, prefix S3 %s",
                    dataSourceId, localPath, prefixForS3Key));
        }

        //String localPath = dataSourceConfig.hasModCheckFilename() ? dataSourceConfig.getModTimeCheckFilename() : dataSourceConfig.getDownloadFilename();

        logger.info(format("Upload data source: '%s' path: '%s' prefix: '%s'", dataSourceId, localPath, prefixForS3Key));

        boolean result;
        if (noCompressionNeeded(localPath.toString())) {
            result = uploadFileToS3.uploadFile(prefixForS3Key, localPath, true);
        } else {
            String s3Key = dataSourceConfig.getDownloadFilename();
            if (!isZip(s3Key)) {
                logger.warn(format("datasource %s uploadkey %s (from downloadFilename config) does not end with .zip, will add",
                        dataSourceId, s3Key));
                s3Key = s3Key + ".zip";
            }
            logger.info(format("Uploading with compression enabled localPath %s and S3 key %s", localPath, s3Key));
            result = uploadFileToS3.uploadFileZipped(prefixForS3Key, localPath, true, s3Key);
        }

        if (!result) {
            logger.error("Unable to upload for " + dataSourceId + " check above logs for failures");
        }
        return result;
    }

    private static boolean noCompressionNeeded(String sourceFilename) {
        return isZip(sourceFilename) || sourceFilename.toLowerCase().endsWith(".txt");
    }

    private static boolean isZip(String sourceFilename) {
        return sourceFilename.toLowerCase().endsWith(".zip");
    }
}
