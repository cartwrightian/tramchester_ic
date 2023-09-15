package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DateRangeAndVersion;

@ImplementedBy(TransportData.class)
public interface ProvidesFeedInfo {

    DateRangeAndVersion getDateRangeAndVersionFor(DataSourceID dataSourceID);

    boolean hasDateRangeAndVersionFor(DataSourceID dataSourceID);
}
