package com.tramchester.healthchecks;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DateRangeAndVersion;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static java.lang.String.format;

public class DataExpiryHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(DataExpiryHealthCheck.class);

    private final DateRangeAndVersion feedInfo;
    private final TramchesterConfig config;
    private final DataSourceID dataSourceID;
    private final ProvidesNow providesNow;

    public DataExpiryHealthCheck(DateRangeAndVersion rangeAndVersion, DataSourceID dataSourceID, ProvidesNow providesNow, TramchesterConfig config,
                                 ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.feedInfo = rangeAndVersion;
        this.dataSourceID = dataSourceID;
        this.providesNow = providesNow;
        this.config = config;
    }

    @Override
    public Result check() {
        return checkForDate(providesNow.getDate());
    }

    public Result checkForDate(LocalDate currentDate) {
        int days = config.getDataExpiryThreadhold();

        LocalDate validUntil = feedInfo.validUntil();

        if (validUntil==null) {
            String msg = "Cannot check data expiry, no 'valid until' present in feedinfo";
            logger.warn(msg);
            return Result.unhealthy(msg);
        }

        logger.info(format("Checking if %s data is expired or will expire with %d days of %s", dataSourceID, days, validUntil));

        if (currentDate.isAfter(validUntil) || currentDate.isEqual(validUntil)) {
            String message = dataSourceID + " data expired on " + validUntil;
            logger.error(message);
            return Result.unhealthy(message);
        }

        LocalDate boundary = validUntil.minusDays(days);
        if (currentDate.isAfter(boundary) || currentDate.isEqual(boundary)) {
            String message = dataSourceID + " data will expire on " + validUntil;
            return Result.unhealthy(message);
        }

        String message = dataSourceID + " data is not due to expire until " + validUntil;
        logger.info(message);
        return Result.healthy(message);
    }

    @Override
    public String getName() {
        return "dataExpiry"+dataSourceID.name();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
