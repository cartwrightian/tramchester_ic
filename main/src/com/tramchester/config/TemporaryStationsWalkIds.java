package com.tramchester.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.DateRange;

@JsonDeserialize( as = TemporaryStationsWalkIdsConfig.class)
public interface TemporaryStationsWalkIds {
    DateRange getDateRange();
    StationIdPair getStationPair();
}
