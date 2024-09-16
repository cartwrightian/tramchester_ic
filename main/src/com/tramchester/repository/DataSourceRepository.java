package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.reference.TransportMode;

import java.time.ZonedDateTime;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface DataSourceRepository {
    Set<DataSourceInfo> getDataSourceInfo();

    ZonedDateTime getNewestModTimeFor(TransportMode mode);

    boolean hasDataSourceInfo();

    String summariseDataSourceInfo();

    DataSourceInfo getDataSourceInfo(DataSourceID dataSourceID);
}
