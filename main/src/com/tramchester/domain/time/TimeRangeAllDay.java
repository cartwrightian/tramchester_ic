package com.tramchester.domain.time;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TimeRangeAllDay implements TimeRange {

    @JsonCreator
    public TimeRangeAllDay() {
    }

    @Override
    public boolean anyOverlap(TimeRange other) {
        return true;
    }

    @Override
    public boolean intoNextDay() {
        return false;
    }

    @Override
    public TimeRange forFollowingDay() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public TimeRange transposeToNextDay() {
        throw new RuntimeException("not implemented");
    }

    @JsonIgnore
    @Override
    public TramTime getStart() {
        return TramTime.of(0,1);
    }

    @JsonIgnore
    @Override
    public boolean allDay() {
        return true;
    }

    @JsonIgnore
    @Override
    public TramTime getEnd() {
        return TramTime.of(0,0);
    }

    @Override
    public void updateToInclude(TramTime callingTime) {
        // no op
    }

    @Override
    public boolean contains(TramTime tramTime) {
        return true;
    }

    @Override
    public boolean fullyContains(TimeRange other) {
        return true;
    }

    @Override
    public String toString() {
        return "TimeRangeAllDay{}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof TimeRangeAllDay;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
