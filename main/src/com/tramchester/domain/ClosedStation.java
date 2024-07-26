package com.tramchester.domain;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

import java.util.Objects;
import java.util.Set;

public class ClosedStation {
    private final Station station;
    private final DateRange dateRange;
    private final boolean fullyClosed;
    private final Set<Station> diversionsAroundClosure;
    private final Set<Station> diversionsToFromClosure;

    public ClosedStation(Station station, DateRange dateRange, boolean fullyClosed, Set<Station> diversionsAroundClosure, Set<Station> diversionsToFromClosure) {
        this.station = station;
        this.dateRange = dateRange;
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

    public boolean isFullyClosed() {
        return fullyClosed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClosedStation that = (ClosedStation) o;
        return fullyClosed == that.fullyClosed && station.equals(that.station) && dateRange.equals(that.dateRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station, dateRange, fullyClosed, diversionsAroundClosure, diversionsToFromClosure);
    }

    @Override
    public String toString() {
        return "ClosedStation{" +
                "station=" + station.getId() +
                ", dateRange=" + dateRange +
                ", fullyClosed=" + fullyClosed +
                ", diversionsAroundClosure=" + HasId.asIds(diversionsAroundClosure) +
                ", diversionsToFromClosure=" + HasId.asIds(diversionsToFromClosure) +
                '}';
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
}
