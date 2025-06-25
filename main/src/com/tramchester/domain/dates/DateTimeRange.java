package com.tramchester.domain.dates;

import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;

import java.util.Objects;

public class DateTimeRange {
    private final DateRange dateRange;
    private final TimeRange timeRange;

    public DateTimeRange(TramDate begin, TramDate end) {
        this(new DateRange(begin,end), TimeRange.AllDay());
    }

    public DateTimeRange(DateRange dateRange, TimeRange timeRange) {
        this.dateRange = dateRange;
        this.timeRange = timeRange;
    }

    public DateTimeRange(TramDate begin, TramDate end, TramTime start, TramTime finish) {
        this(DateRange.of(begin,end), TimeRange.of(start, finish));
    }

    public static DateTimeRange of(DateRange dateRange, TimeRange timeRange) {
        return new DateTimeRange(dateRange, timeRange);
    }

    public static DateTimeRange of(TramDate date, TimeRange timeRange) {
        return of(DateRange.of(date,0), timeRange);
    }

    public boolean contains(TramDate date, TramTime time) {
        if (dateRange.contains(date)) {
            return timeRange.contains(time);
        }
        return false;
    }

    public boolean contains(TramDate date) {
        return dateRange.contains(date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateTimeRange range)) return false;
        return Objects.equals(dateRange, range.dateRange) && Objects.equals(timeRange, range.timeRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateRange, timeRange);
    }

    @Override
    public String toString() {
        return "DateTimeRange{" +
                dateRange +
                ", " + timeRange +
                '}';
    }

    public boolean overlaps(DateTimeRange other) {
        if (dateRange.overlapsWith(other.dateRange)) {
            return timeRange.anyOverlap(other.timeRange);
        } else {
            return false;
        }
    }

    public boolean fullyContains(TimeRange timeRange) {
        return this.timeRange.fullyContains(timeRange);
    }

    public boolean allDay() {
        return timeRange.allDay();
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

}
