package com.tramchester.domain.closures;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;

import java.util.Objects;
import java.util.Set;

public class ClosedStation {
    private final Station station;
    private final DateRange dateRange;
    private final TimeRange timeRange;
    private final boolean fullyClosed;
    private final Set<Station> diversionsAroundClosure;
    private final Set<Station> diversionsToFromClosure;

    public ClosedStation(Station station, DateRange dateRange, boolean fullyClosed, Set<Station> diversionsAroundClosure,
                         Set<Station> diversionsToFromClosure) {
        this(station, dateRange, TimeRange.AllDay(), fullyClosed, diversionsAroundClosure, diversionsToFromClosure);
    }

    public ClosedStation(Station station, DateRange dateRange, TimeRange timeRange, boolean fullyClosed, Set<Station> diversionsAroundClosure,
                         Set<Station> diversionsToFromClosure) {
        this.station = station;
        this.dateRange = dateRange;
        this.timeRange = timeRange;
        this.fullyClosed = fullyClosed;
        this.diversionsAroundClosure = diversionsAroundClosure;
        this.diversionsToFromClosure = diversionsToFromClosure;
    }

    public Station getStation() {
        return station;
    }

    public IdFor<Station> getStationId() {
        return station.getId();
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    // TODO Is this still needed
    public boolean isFullyClosed() {
        return fullyClosed;
    }

    /***
     * @return Stations that should be linked together to provide a diversion around the closure
     */
    public Set<Station> getDiversionAroundClosure() {
        return diversionsAroundClosure;
    }

    /***
     * @return stations that should to linked to/from the closure station to give an alternative path
     */
    public Set<Station> getDiversionToFromClosure() {
        return diversionsToFromClosure;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClosedStation that)) return false;
        return isFullyClosed() == that.isFullyClosed() && Objects.equals(getStation(), that.getStation()) && Objects.equals(getDateRange(), that.getDateRange()) && Objects.equals(getTimeRange(), that.getTimeRange()) && Objects.equals(diversionsAroundClosure, that.diversionsAroundClosure) && Objects.equals(diversionsToFromClosure, that.diversionsToFromClosure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStation(), getDateRange(), getTimeRange(), isFullyClosed(), diversionsAroundClosure, diversionsToFromClosure);
    }

    @Override
    public String toString() {
        return "ClosedStation{" +
                "station=" + station.getId() +
                ", dateRange=" + dateRange +
                ", timeRange=" + timeRange +
                ", fullyClosed=" + fullyClosed +
                ", diversionsAroundClosure=" + HasId.asIds(diversionsAroundClosure) +
                ", diversionsToFromClosure=" + HasId.asIds(diversionsToFromClosure) +
                '}';
    }

    public boolean overlaps(final ClosedStation other) {
        if (!other.dateRange.overlapsWith(dateRange)) {
            return false;
        }
        return other.timeRange.anyOverlap(timeRange);
    }
}
