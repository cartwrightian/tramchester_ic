package com.tramchester.domain.time;

import java.util.Objects;

public class TimeRangeAllDay implements TimeRange {

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

    @Override
    public TramTime getStart() {
        return TramTime.of(0,1);
    }

    @Override
    public boolean allDay() {
        return true;
    }

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


}
