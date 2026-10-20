package com.tramchester.domain.dates;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.serialisation.TramDateJsonDeserializer;
import com.tramchester.mappers.serialisation.TramDateJsonSerializer;

import java.util.*;
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

    @JsonCreator
    public static DateRange of(@JsonProperty("start") @JsonDeserialize(using = TramDateJsonDeserializer.class) TramDate startDate,
                               @JsonProperty("end") @JsonDeserialize(using = TramDateJsonDeserializer.class)  TramDate endDate) {
        return new DateRange(startDate, endDate);
    }

    public static DateRange Empty() {
        return Empty;
    }

    /***
     * Finds the first and last date in the collection and creates a range based on those
     * @param dates a collection of dates
     * @return the computed range
     */
    public static DateRange from(final Collection<TramDate> dates) {
        final List<TramDate> sorted = dates.stream().sorted(TramDate::compareTo).toList();

        return new DateRange(sorted.getFirst(), sorted.getLast());
    }

    public static DateRange of(TramDate begin, int durationDays) {
        return of(begin, begin.plusDays(durationDays));
    }

    public static DateRange leastOverlap(final Set<DateRange> ranges) {
        if (ranges.isEmpty()) {
            throw new RuntimeException("Empty set");
        }

        final List<DateRange> sorted = new ArrayList<>(ranges);
        if (ranges.size()==1) {
            return sorted.getFirst();
        }
        // now sort, shortest range first
        sorted.sort(Comparator.comparingLong(DateRange::numberOfDays));

        final DateRange first = sorted.getFirst();
        if (sorted.size()==1) {
            return first;
        }

        DateRange result = first;
        for (int i = 1; i < sorted.size(); i++) {
            final DateRange next = sorted.get(i);
            if (!result.overlapsWith(next)) {
                throw new RuntimeException("no overlap between %s and %s from %s".formatted(result, next, ranges));
            }
            result = result.createOverlapWith(next);
        }

        return result;

    }

    /***
     * Returns a new DateRange with only the overlap
     * @param other the other DateRange to create the overlap with
     * @return the DateRange where this and other overlap
     * Throws if no overlap
     */
    public DateRange createOverlapWith(final DateRange other) {
        if (!this.overlapsWith(other)) {
            throw new RuntimeException("No overlap between %s and %s".formatted(this, other));
        }
        final TramDate overlapStart;
        if (other.startDate.isAfter(startDate)) {
            overlapStart = other.startDate;
        } else {
            overlapStart = startDate;
        }
        final TramDate overlapEnd;
        if (other.endDate.isBefore(endDate)) {
            overlapEnd = other.endDate;
        } else {
            overlapEnd = endDate;
        }
        return DateRange.of(overlapStart, overlapEnd);

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

    @JsonIgnore
    public boolean isEmpty() {
        return this==Empty;
    }

    private static boolean between(final DateRange dateRange, final TramDate date) {
        if (date.equals(dateRange.startDate) || date.equals(dateRange.endDate)) {
            return true;
        }
        return (date.isAfter(dateRange.startDate)  && date.isBefore(dateRange.endDate));
    }

    @JsonProperty("end")
    @JsonSerialize(using = TramDateJsonSerializer.class)
    public TramDate getEndDate() {
        guardForEmpty();
        return endDate;
    }

    private void guardForEmpty() {
        if (isEmpty()) {
            throw new RuntimeException("Empty range");
        }
    }

    @JsonProperty("start")
    @JsonSerialize(using = TramDateJsonSerializer.class)
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
