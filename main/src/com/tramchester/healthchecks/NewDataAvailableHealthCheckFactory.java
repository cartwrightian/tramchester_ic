package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.HttpDownloadAndModTime;
import com.tramchester.dataimport.S3DownloadAndModTime;
import com.tramchester.domain.ServiceTimeLimits;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@LazySingleton
public class NewDataAvailableHealthCheckFactory implements HealthCheckFactory {

    private final TramchesterConfig config;
    private final HttpDownloadAndModTime httpDownloadAndModTime;
    private final S3DownloadAndModTime s3DownloadAndModTime;
    private final GetsFileModTime fileModTime;
    private final List<TramchesterHealthCheck> healthCheckList;
    private final ServiceTimeLimits serviceTimeLimits;

    @Inject
    public NewDataAvailableHealthCheckFactory(TramchesterConfig config, HttpDownloadAndModTime httpDownloadAndModTime,
                                              S3DownloadAndModTime s3DownloadAndModTime, GetsFileModTime fileModTime,
                                              ServiceTimeLimits serviceTimeLimits) {
        this.config = config;
        this.httpDownloadAndModTime = httpDownloadAndModTime;
        this.s3DownloadAndModTime = s3DownloadAndModTime;
        this.fileModTime = fileModTime;
        this.serviceTimeLimits = serviceTimeLimits;
        healthCheckList = new ArrayList<>();
    }

    public Collection<TramchesterHealthCheck> getHealthChecks() {
        return healthCheckList;
    }

    @PreDestroy
    public void dispose() {
        healthCheckList.clear();
    }

    @PostConstruct
    public void start() {
        config.getRemoteDataSourceConfig().stream().
                filter(source -> !source.getDataCheckUrl().isBlank()).forEach(config ->
                healthCheckList.add(new NewDataAvailableHealthCheck(config, httpDownloadAndModTime, s3DownloadAndModTime,
                        fileModTime, serviceTimeLimits)));
    }

}
