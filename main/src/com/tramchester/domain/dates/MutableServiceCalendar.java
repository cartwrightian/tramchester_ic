package com.tramchester.domain.dates;

public interface MutableServiceCalendar extends ServiceCalendar {
    void cancel();

    void includeExtraDate(TramDate date);

    void excludeDate(TramDate date);
}
