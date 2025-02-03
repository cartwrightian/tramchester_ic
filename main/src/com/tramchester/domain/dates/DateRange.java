package com.tramchester.domain.dates;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DateRange {
    private final TramDate startDate;
    private final TramDate endDate;

    private static final DateRange Empty = of(null, null);

    public DateRange(TramDate startDate, TramDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static DateRange of(TramDate startDate, TramDate endDate) {
        return new DateRange(startDate, endDate);
    }

    public static DateRange Empty() {
        return Empty;
    }

    public static DateRange from(final Collection<TramDate> dates) {
        final List<TramDate> sorted = dates.stream().sorted(TramDate::compareTo).toList();

        return new DateRange(sorted.getFirst(), sorted.getLast());
    }

    public boolean contains(final TramDate queryDate) {
        if (isEmpty()) {
            return false;
        }
        if (queryDate.isAfter(endDate) || queryDate.isBefore(startDate)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateRange dateRange = (DateRange) o;
        return startDate.equals(dateRange.startDate) && endDate.equals(dateRange.endDate);
    }

    @Override
    public int hashCode() {
        guardForEmpty();
        return Objects.hash(startDate, endDate);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "DateRange{EMPTY}";
        }

        return "DateRange{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }

    public boolean overlapsWith(final DateRange other) {
        if (other.isEmpty() || this.isEmpty()) {
            return false;
        }
        return between(other, startDate) ||
                between(other, endDate) ||
                between(this, other.startDate) ||
                between(this, other.endDate);
    }

    public boolean isEmpty() {
        return this==Empty;
    }

    private static boolean between(final DateRange dateRange, final TramDate date) {
        if (date.equals(dateRange.startDate) || date.equals(dateRange.endDate)) {
            return true;
        }
        return (date.isAfter(dateRange.startDate)  && date.isBefore(dateRange.endDate));
    }

    public TramDate getEndDate() {
        guardForEmpty();
        return endDate;
    }

    private void guardForEmpty() {
        if (isEmpty()) {
            throw new RuntimeException("Empty range");
        }
    }

    public TramDate getStartDate() {
        guardForEmpty();
        return startDate;
    }

    /***
     * In order stream from start to end date, inclusive
     * @return stream of dates
     */
    public Stream<TramDate> stream() {
        if (isEmpty()) {
            return Stream.empty();
        }

        long start = startDate.toEpochDay();
        long end = endDate.toEpochDay();
        int range = 1 + Math.toIntExact(Math.subtractExact(end, start));

        return IntStream.range(0, range).boxed().
                map(startDate::plusDays).sorted();

    }

    public long numberOfDays() {
        if (isEmpty()) {
            return 0;
        }
        final long diff = Math.subtractExact(endDate.toEpochDay(), startDate.toEpochDay());
        // inclusive, so add one
        return Math.abs(diff+1);
    }
}
