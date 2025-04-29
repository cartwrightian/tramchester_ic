package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.StationsWithDiversion;

import java.util.Set;

@ImplementedBy(StationsWithDiversion.class)
public interface StationsWithDiversionRepository {
    boolean hasDiversions(Station station);
    Set<DateTimeRange> getDateTimeRangesFor(Station station);
}
