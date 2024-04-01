package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ServiceTimeLimits;

import jakarta.inject.Inject;

@LazySingleton
public class SendMetricsToCloudWatchHealthcheck extends TramchesterHealthCheck {

    private final SendMetricsToCloudWatch sendMetricsToCloudWatch;
    private final TramchesterConfig config;

    @Inject
    public SendMetricsToCloudWatchHealthcheck(SendMetricsToCloudWatch sendMetricsToCloudWatch,
                                              ServiceTimeLimits serviceTimeLimits, TramchesterConfig config) {
        super(serviceTimeLimits);
        this.sendMetricsToCloudWatch = sendMetricsToCloudWatch;
        this.config = config;
    }

    @Override
    public String getName() {
        return "sendMetricsToCloudWatch";
    }

    @Override
    public boolean isEnabled() {
        return config.getSendCloudWatchMetrics();
    }

    @Override
    protected Result check() {
        if (!config.getSendCloudWatchMetrics()) {
            return Result.healthy("Disabled in config");
        }

        if (sendMetricsToCloudWatch.started()) {
            return Result.healthy();
        }

        return Result.unhealthy("Unable to upload cloud watch metrics");
    }
}
