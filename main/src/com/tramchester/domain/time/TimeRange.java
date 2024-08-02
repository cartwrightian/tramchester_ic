package com.tramchester.domain.time;

import java.util.Set;

public interface TimeRange {
    boolean anyOverlap(TimeRange other);

    boolean intoNextDay();

    TimeRange forFollowingDay();

    TimeRange transposeToNextDay();

    TramTime getEnd();

    TramTime getStart();

    static TimeRange AllDay() {
        return new TimeRangeAllDay();
    }

    static TimeRange coveringAllOf(Set<TimeRange> ranges) {
        if (ranges.isEmpty()) {
            throw new RuntimeException("No time ranges supplied");
        }
        TramTime earliest = TramTime.of(23,59);
        TramTime latest = TramTime.of(0,1);
        for(final TimeRange range : ranges) {
            if (range.allDay()) {
                throw new RuntimeException("not implemented yet");
            }
            if (range.getStart().isBefore(earliest)) {
                earliest = range.getStart();
            }
            if (range.getEnd().isAfter(latest)) {
                latest = range.getEnd();
            }
        }
        return TimeRangePartial.of(earliest, latest);
    }

    boolean allDay();

    void updateToInclude(TramTime callingTime);

    boolean contains(TramTime tramTime);
}
