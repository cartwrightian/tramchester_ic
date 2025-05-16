package com.tramchester.domain.dates;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Dates {
    private final Set<DateRange> ranges;
    private final Set<TramDate> dates;

    public Dates( Set<TramDate> dates, DateRange... ranges) {
        this.dates = dates;
        this.ranges = new HashSet<>();
        this.ranges.addAll(Arrays.asList(ranges));
    }

    public static Dates of(DateRange... ranges) {
        return new Dates(new HashSet<>(), ranges);
    }

    public static Dates of(Set<TramDate> dates) {
        return new Dates(dates);
    }

    public Dates add(TramDate date) {
        dates.add(date);
        return this;
    }

    public Dates add(DateRange range) {
        ranges.add(range);
        return this;
    }

    public boolean contains(final TramDate date) {
        if (dates.contains(date)) {
            return true;
        }
        return ranges.stream().anyMatch(dateRange -> dateRange.contains(date));
    }

}
