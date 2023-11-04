package com.tramchester.healthchecks;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.HttpDownloadAndModTime;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.domain.ServiceTimeLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;

public class NewDataAvailableHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(NewDataAvailableHealthCheck.class);

    private final RemoteDataSourceConfig config;
    private final HttpDownloadAndModTime urlDownloader;
    private final GetsFileModTime getsFileModTime;

    public NewDataAvailableHealthCheck(RemoteDataSourceConfig config, HttpDownloadAndModTime urlDownloader,
                                       GetsFileModTime getsFileModTime, ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.config = config;
        this.urlDownloader = urlDownloader;
        this.getsFileModTime = getsFileModTime;
    }

    @Override
    protected Result check() {
        URI dataCheckUrl = URI.create(config.getDataCheckUrl());

        try {
            LocalDateTime localFileModTime = getsFileModTime.getFor(config);

            final URLStatus status = urlDownloader.getStatusFor(dataCheckUrl, localFileModTime, config.isMandatory());

            if (!status.isOk()) {
                String msg = String.format("Got http status %s for %s", status.getStatusCode(), dataCheckUrl);
                logger.info(msg);
                return Result.unhealthy(msg);
            }

            LocalDateTime serverModTime = status.getModTime();

            String diag = String.format("Local zip mod time: %s Server mod time: %s Url: %s", localFileModTime, serverModTime, dataCheckUrl);
            if (serverModTime.isAfter(localFileModTime)) {
                String msg = "Newer data is available " + diag;
                logger.warn(msg);
                return Result.unhealthy(msg);
            } else if (serverModTime.equals(LocalDateTime.MIN)) {
                String msg = "No mod time was available from server for " + dataCheckUrl;
                logger.error(msg);
                return Result.unhealthy(msg);
            } else {
                String msg = "No newer data is available " + diag;
                logger.info(msg);
                return Result.healthy(msg);
            }
        } catch (IOException | InterruptedException exception) {
            String msg = "Unable to check for newer data at " + dataCheckUrl;
            logger.error(msg, exception);
            return Result.unhealthy(msg + exception.getMessage());
        }
    }

    @Override
    public String getName() {
        return "new data for " + config.getName();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
