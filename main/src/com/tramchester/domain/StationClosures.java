package com.tramchester.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.config.StationClosuresConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.time.TimeRange;

@JsonDeserialize(as= StationClosuresConfig.class)
public interface StationClosures {

    IdSet<Station> getStations();
    boolean isFullyClosed();
    DateRange getDateRange();

    boolean hasTimeRange();
    TimeRange getTimeRange();

    @JsonIgnore
    boolean hasDiversionsAroundClosure();

    IdSet<Station> getDiversionsAroundClosure();

    @JsonIgnore
    boolean hasDiversionsToFromClosure();

    IdSet<Station> getDiversionsToFromClosure();

    static boolean areEqual(StationClosures a, Object other) {
        if (!StationClosures.class.isAssignableFrom(other.getClass())) {
            return false;
        }
        StationClosures b = (StationClosures) other;
        boolean requiredFieldsSame = a.getStations().equals(b.getStations()) &&
                (a.isFullyClosed()==b.isFullyClosed()) &&
                (a.getDateRange().equals(b.getDateRange()));
//                (a.hasTimeRange()==b.hasTimeRange()) &&
//                (a.hasDiversionsAroundClosure() == b.hasDiversionsAroundClosure()) &&
//                (a.hasDiversionsToFromClosure() == b.hasDiversionsToFromClosure()));
        if (!requiredFieldsSame) {
            return false;
        }
        if (a.hasTimeRange()) {
            if (!b.hasTimeRange()) {
                return false;
            }
            if (!a.getTimeRange().equals(b.getTimeRange())) {
                return false;
            }
        }
        if (a.hasDiversionsToFromClosure()) {
            if (!b.hasDiversionsToFromClosure()) {
                return false;
            }
            if (!a.getDiversionsToFromClosure().equals(b.getDiversionsToFromClosure())) {
                return false;
            }
        }
        if (a.hasDiversionsAroundClosure()) {
            if (!b.hasDiversionsAroundClosure()) {
                return false;
            }
            if (!a.getDiversionsAroundClosure().equals(b.getDiversionsAroundClosure())) {
                return false;
            }
        }
        return true;
    }
}
