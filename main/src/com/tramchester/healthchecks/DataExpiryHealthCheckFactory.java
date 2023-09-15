package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DateRangeAndVersion;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.TransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@LazySingleton
public class DataExpiryHealthCheckFactory implements HealthCheckFactory {
    private static final Logger logger = LoggerFactory.getLogger(DataExpiryHealthCheckFactory.class);

    private final List<TramchesterHealthCheck> healthChecks;
    private final TransportData transportData;
    private final ProvidesNow providesNow;
    private final TramchesterConfig config;
    private final ServiceTimeLimits serviceTimeLimits;

    @Inject
    public DataExpiryHealthCheckFactory(TransportData transportData, ProvidesNow providesNow,
                                        TramchesterConfig config, ServiceTimeLimits serviceTimeLimits) {
        this.transportData = transportData;
        this.providesNow = providesNow;
        this.config = config;
        this.serviceTimeLimits = serviceTimeLimits;
        healthChecks = new ArrayList<>();
    }

    @Override
    public Collection<TramchesterHealthCheck> getHealthChecks() {
        return healthChecks;
    }

    @PreDestroy
    public void dispose() {
        healthChecks.clear();
    }

    @PostConstruct
    public void start() {

        for (DataSourceID dataSourceID : DataSourceID.values()) {
            if (transportData.hasDateRangeAndVersionFor(dataSourceID)) {
                DateRangeAndVersion dateRangeAndVersion = transportData.getDateRangeAndVersionFor(dataSourceID);
                TramchesterHealthCheck healthCheck = new DataExpiryHealthCheck(dateRangeAndVersion, dataSourceID, providesNow, config, serviceTimeLimits);
                healthChecks.add(healthCheck);
            } else
            {
                logger.warn("Cannot add healthcheck for " + dataSourceID);
            }
        }

//        transportData.getDateRangesAndVersions().forEach((name, feedInfo) -> {
//            if (feedInfo.validUntil()!=null) {
//                TramchesterHealthCheck healthCheck = new DataExpiryHealthCheck(feedInfo, name, providesNow, config, serviceTimeLimits);
//                healthChecks.add(healthCheck);
//            } else {
//                logger.warn("Cannot add healthcheck for " + feedInfo + " since 'valid until' is missing");
//            }
//        });
    }

}
