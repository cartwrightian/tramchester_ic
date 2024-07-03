package com.tramchester.domain;

import com.tramchester.domain.dates.DateRange;

public interface TemporaryStationsWalk {
    DateRange getDateRange();

    StationIdPair getStationPair();
}
