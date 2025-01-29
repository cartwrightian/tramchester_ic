package com.tramchester.domain.closures;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;

import java.util.Set;

public class Closure {
    private final DateTimeRange dateTimeRange;
    private final Set<Station> stations;
    private final boolean fullyClosed;

    public Closure(DateTimeRange dateTimeRange, Set<Station> stations, boolean fullyClosed) {
        this.dateTimeRange = dateTimeRange;

        this.stations = stations;
        this.fullyClosed = fullyClosed;
    }

    public boolean overlapsWith(final DateRange dateRange) {
        return dateRange.overlapsWith(dateRange);
    }

    public boolean activeFor(final TramDate date) {
        return dateTimeRange.contains(date);
    }

    public DateRange getDateRange() {
        return dateTimeRange.getDateRange();
    }

    public TimeRange getTimeRange() {
        return dateTimeRange.getTimeRange();
    }

    public Set<Station> getStations() {
        return stations;
    }

    public boolean isFullyClosed() {
        return fullyClosed;
    }
}
