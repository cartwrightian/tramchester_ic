package com.tramchester.domain.dates;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DateRanges {
    private final Set<DateRange> ranges;

    public DateRanges(DateRange... ranges) {
        this.ranges = new HashSet<>();
        this.ranges.addAll(Arrays.asList(ranges));
    }

    public boolean contains(final TramDate date) {
        return ranges.stream().anyMatch(dateRange -> dateRange.contains(date));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DateRanges that)) return false;
        return Objects.equals(ranges, that.ranges);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ranges);
    }

    @Override
    public String toString() {
        return "DateRanges{" +
                "ranges=" + ranges +
                '}';
    }
}
