package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;

import java.time.LocalDate;
import java.util.Objects;

public class DateRangeConfig {
    private final LocalDate begin;
    private final LocalDate end;

    public DateRangeConfig(@JsonProperty(value = "begin", required = true) LocalDate begin,
                           @JsonProperty(value = "end", required = true) LocalDate end) {
        this.begin = begin;
        this.end = end;
    }

    public DateRange getRange() {
        return DateRange.of(TramDate.of(begin), TramDate.of(end));
    }

    public boolean isValid() {
        return end.isAfter(begin) || end.equals(begin);
    }

    @Override
    public String toString() {
        return "DateRangeConfig{" +
                "begin=" + begin +
                ", end=" + end +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateRangeConfig that = (DateRangeConfig) o;
        return Objects.equals(begin, that.begin) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
    }

}
