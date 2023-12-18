package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@LazySingleton
public class RegistersHealthchecks {
    private static final Logger logger = LoggerFactory.getLogger(RegistersHealthchecks.class);

    private final Set<TramchesterHealthCheck> healthChecks;

    private final Duration cacheDuration = Duration.ofSeconds(10);
    private final ProvidesNow providesNow;

    @Inject
    public RegistersHealthchecks(DataExpiryHealthCheckFactory dataExpiryHealthCheckFactory,
                                 NewDataAvailableHealthCheckFactory newDataAvailableHealthCheckFactory,
                                 GraphHealthCheck graphHealthCheck,
                                 LiveDataHealthCheck liveDataHealthCheck,
                                 LiveDataMessagesHealthCheck liveDataMessagesHealthCheck,
                                 LiveDataS3UploadHealthCheck liveDataS3UploadHealthCheck,
                                 ClientForS3Healthcheck clientForS3Healthcheck,
                                 SendMetricsToCloudWatchHealthcheck sendMetricsToCloudWatchHealthcheck, ProvidesNow providesNow) {
        this.providesNow = providesNow;
        this.healthChecks = new HashSet<>();

        addIfEnabled(dataExpiryHealthCheckFactory.getHealthChecks());
        addIfEnabled(newDataAvailableHealthCheckFactory.getHealthChecks());
        addIfEnabled(graphHealthCheck);
        addIfEnabled(clientForS3Healthcheck);
        addIfEnabled(sendMetricsToCloudWatchHealthcheck);

        // live data health checks
        addIfEnabled(liveDataHealthCheck);
        addIfEnabled(liveDataMessagesHealthCheck);
        addIfEnabled(liveDataS3UploadHealthCheck);
    }


    private void addIfEnabled(Collection<TramchesterHealthCheck> healthChecks) {
        healthChecks.forEach(this::addIfEnabled);
    }

    private void addIfEnabled(TramchesterHealthCheck healthCheck) {
        if (healthCheck.isEnabled()) {
            CachingHealthCheck cachingHealthCheck = new CachingHealthCheck(healthCheck, cacheDuration, providesNow);
            healthChecks.add(cachingHealthCheck);
        } else {
            logger.info(healthCheck.getName() + " not enabled");
        }
    }

    public void registerAllInto(HealthCheckRegistry registry) {
        healthChecks.forEach(healthCheck -> {
            logger.info("Register health check " + healthCheck.getName());
            registry.register(healthCheck.getName(), healthCheck); });
    }
}
