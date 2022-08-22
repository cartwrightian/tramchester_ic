package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

public class TramServiceDate {
    private final LocalDate date;

    public static TramServiceDate of(LocalDate date) {
        return new TramServiceDate(date);
    }

    public TramServiceDate(LocalDate date) {
        this.date = date;
    }

    public TramServiceDate(TramDate date) {
        this.date = date.toLocalDate();
    }

    public LocalDate getDate() {
        return date;
    }

    public String getStringDate() {
        return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Override
    public String toString() {
        return "TramServiceDate{" +
                "date=" + date +
                ", day=" + date.getDayOfWeek().name() +
                '}';
    }

    public boolean isWeekend() {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return (dayOfWeek==DayOfWeek.SATURDAY) || (dayOfWeek==DayOfWeek.SUNDAY);
    }

    public boolean isChristmasPeriod() {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month==Month.DECEMBER && day>23) {
            return true;
        }
        if (month==Month.JANUARY && day<3) {
            return true;
        }
        return false;
    }

    public boolean within(LocalDate from, LocalDate until) {
        if (date.isAfter(until)) {
            return false;
        }
        if (date.isBefore(from)) {
            return false;
        }
        return true;
    }
}
