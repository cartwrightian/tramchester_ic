package com.tramchester.testSupport;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.reference.TramStations;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class UpcomingDates {

    private static final TramDate sunday;
    private static final TramDate saturday;
    private static final TramDate monday;

    // use helper methods that handle filtering (i.e. for Christmas) and conversion to dates
    static final int DAYS_AHEAD = 7;

//    public static TramDate ChristmasParadeDate = TramDate.of(2024, 12, 8);
//    public static TimeRange ChristmasParadeTiming = TimeRange.of(TramTime.of(12,0),
//            TramTime.of(14,30));

    public static TramDate VictoriaBuryImprovementWorks = TramDate.of(2025,1,19);
    public static TimeRange VictoriaBuryImprovementWorksTiming = TimeRange.of(TramTime.of(4,0),
            TramTime.of(10,0));

    static {
        TramDate today = TramDate.from(TestEnv.LocalNow());
        sunday = getNextDate(DayOfWeek.SUNDAY, today);
        saturday = getNextDate(DayOfWeek.SATURDAY, today);
        monday = getNextDate(DayOfWeek.MONDAY, today);
    }

    public static boolean hasClosure(final Station station, final TramDate date) {
        return hasClosure(station.getId(), date);
    }

    private static boolean hasClosure(IdFor<Station> stationId, TramDate date) {
        return false;
    }

    public static boolean hasClosure(TramStations station, TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean validTestDate(final TramDate date) {
        return !date.isChristmasPeriod();
    }

    public static List<TramDate> daysAhead() {
        TramDate date = TramDate.of(TestEnv.LocalNow().toLocalDate()).plusDays(1);

        final List<TramDate> dates= new ArrayList<>();
        while (dates.size() <= DAYS_AHEAD) {
            if (validTestDate(date)) {
                dates.add(date);
            }
            date = date.plusDays(1);
        }

        return dates;
    }

    public static Stream<TramDate> getUpcomingDates() {
        return daysAhead().stream();
    }

    public static TramDate nextSunday() {
        TramDate result = sunday;
        while (!validTestDate(result)) {
            result = result.plusWeeks(1);
        }
        return result;
    }

    public static TramDate nextSaturday() {
        return saturday;
    }

    public static TramDate nextMonday() {
        return monday;
    }


    static TramDate getNextDate(DayOfWeek dayOfWeek, TramDate date) {
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return avoidChristmasDate(date);
    }

    public static TramDate avoidChristmasDate(final TramDate startDate) {
        TramDate date = startDate;
        while (date.isChristmasPeriod()) {
            date = date.plusWeeks(1);
        }
        return date;
    }


    public static boolean isChristmasDay(TramDate date) {
        LocalDate localDate = date.toLocalDate();
        return localDate.getMonth().equals(Month.DECEMBER) && localDate.getDayOfMonth()==25;
    }

    public static boolean isBoxingDay(TramDate date) {
        LocalDate localDate = date.toLocalDate();
        return localDate.getMonth().equals(Month.DECEMBER) && localDate.getDayOfMonth()==26;
    }
}
