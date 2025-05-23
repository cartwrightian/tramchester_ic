package com.tramchester.domain.time;

import java.time.Duration;
import java.util.Objects;

import static java.lang.String.format;

public class TimeRangePartial implements TimeRange {
    private TramTime begin;
    private TramTime end;

    private TimeRangePartial(final TramTime begin, final TramTime end) {
        this.begin = begin;
        this.end = end;
        if (end.isBefore(begin)) {
            throw new RuntimeException(format("End time %s is before begin %s", end, begin));
        }
        if (begin.isNextDay() && !end.isNextDay()) {
            throw new RuntimeException(format("Begin time %s is next day but end is not %s", begin, end));
        }
    }

    public static TimeRange of(final TramTime first, final TramTime second) {
        return new TimeRangePartial(first, second);
    }

    public static TimeRange of(final TramTime time) {
        return new TimeRangePartial(time);
    }

    // supports use of updateToInclude
    private TimeRangePartial(final TramTime tramTime) {
        begin = tramTime;
        end = tramTime;
    }

    @Override
    public void updateToInclude(final TramTime callingTime) {
        if (callingTime.isBefore(begin)) {
            begin = callingTime;
            return;
        }
        if (callingTime.isAfter(end)) {
            end = callingTime;
        }
    }

    public static TimeRange of(final TramTime time, final Duration before, final Duration after) {
        Duration calcBefore = before;
        if (time.getHourOfDay()==0 && !time.isNextDay()) {
            final Duration currentMinsOfDay = Duration.ofMinutes(time.getMinuteOfHour());
            if (Durations.greaterThan(before, currentMinsOfDay)) {
                calcBefore = currentMinsOfDay;
            }
        }
        final TramTime begin = time.minus(calcBefore);
        final TramTime end = time.plus(after);
        return new TimeRangePartial(begin, end);
    }


    public boolean contains(final TramTime time) {
        return !time.isBefore(begin) && !time.isAfter(end);

//        if ((time.equals(begin)) || time.isAfter(begin)) {
//            return (time.equals(end) || time.isBefore(end));
//        }
//        return false;
    }

    @Override
    public boolean fullyContains(final TimeRange other) {
        if (other.allDay()) {
            return false; // since this is partial
        }
        final TramTime otherBegin = other.getStart();
        if (otherBegin.isBefore(begin) || otherBegin.isAfter(end)) {
            return false;
        }
        final TramTime otherEnd = other.getEnd();
        if (otherEnd.isBefore(begin) || otherEnd.isAfter(end)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TimeRange{" +
                "begin=" + begin +
                ", end=" + end +
                '}';
    }

    @Override
    public boolean anyOverlap(final TimeRange other) {

        if (contains(other.getStart()) || contains(other.getEnd())) {
            return true;
        }

        return other.contains(begin) || other.contains(end);
    }

    @Override
    public boolean intoNextDay() {
        return end.isNextDay();
    }

    @Override
    public TimeRange forFollowingDay() {
        if (!end.isNextDay()) {
            throw new RuntimeException("Does not contain a next day element: " + this);
        }
        TramTime endTime = TramTime.of(end.getHourOfDay(), end.getMinuteOfHour());
        return TimeRangePartial.of(TramTime.of(0,0), endTime);
    }

    /***
     * effectively moves range to same range but for the following day
     * @return range transposed into following day
     */
    @Override
    public TimeRange transposeToNextDay() {
        if (intoNextDay()) {
            throw new RuntimeException("Cannot call for a range that is already into following day");
        }
        return TimeRangePartial.of(TramTime.nextDay(begin), TramTime.nextDay(end));
    }

    @Override
    public TramTime getEnd() {
        return end;
    }

    @Override
    public TramTime getStart() {
        return begin;
    }

    @Override
    public boolean allDay() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeRangePartial that)) return false;
        return Objects.equals(begin, that.begin) && Objects.equals(getEnd(), that.getEnd());
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, getEnd());
    }
}
