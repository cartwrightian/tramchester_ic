package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.config.StationClosuresConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.dates.DateRange;

@JsonDeserialize(as= StationClosuresConfig.class)
public interface StationClosures {

    IdSet<Station> getStations();
    TramDate getBegin();
    TramDate getEnd();
    boolean isFullyClosed();

    DateRange getDateRange();

    // only add diversions for these stations, not automatically for all
    IdSet<Station> getDiversionsOnly();
}
