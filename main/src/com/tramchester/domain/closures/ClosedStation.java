package com.tramchester.domain.closures;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;

import java.util.Objects;
import java.util.Set;

public class ClosedStation {
    private final Station station;
    private final DateTimeRange dateTimeRange;

    private final Set<Station> diversionsAroundClosure;
    private final Set<Station> diversionsToFromClosure;

    public ClosedStation(Station station, DateRange dateRange, Set<Station> diversionsAroundClosure,
                         Set<Station> diversionsToFromClosure) {
        this(station, dateRange, TimeRange.AllDay(), diversionsAroundClosure, diversionsToFromClosure);
    }

    public ClosedStation(Station station, DateRange dateRange, TimeRange timeRange, Set<Station> diversionsAroundClosure,
                         Set<Station> diversionsToFromClosure) {
        this.station = station;
        this.dateTimeRange = DateTimeRange.of(dateRange, timeRange);
        this.diversionsAroundClosure = diversionsAroundClosure;
        this.diversionsToFromClosure = diversionsToFromClosure;
    }

    public Station getStation() {
        return station;
    }

    public IdFor<Station> getStationId() {
        return station.getId();
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClosedStation that)) return false;
        return Objects.equals(getStation(), that.getStation()) && Objects.equals(getDateTimeRange(), that.getDateTimeRange())
                && Objects.equals(diversionsAroundClosure, that.diversionsAroundClosure) && Objects.equals(diversionsToFromClosure, that.diversionsToFromClosure);
    }

    public DateTimeRange getDateTimeRange() {
        return dateTimeRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStation(), getDateTimeRange(), diversionsAroundClosure, diversionsToFromClosure);
    }

    @Override
    public String toString() {
        return "ClosedStation{" +
                "station=" + station.getId() +
                ", dateTimeRange=" + dateTimeRange +
                ", diversionsAroundClosure=" + HasId.asIds(diversionsAroundClosure) +
                ", diversionsToFromClosure=" + HasId.asIds(diversionsToFromClosure) +
                '}';
    }

    public boolean closedWholeDay() {
        return dateTimeRange.allDay();
    }

}
