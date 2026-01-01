package com.tramchester.config;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.TramDuration;

public interface TransportDataSourceConfig {
    boolean getOnlyMarkedInterchanges();

    DataSourceID getDataSourceId();

    TramDuration getMaxInitialWait();
}
