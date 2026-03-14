package com.tramchester.testSupport;

import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.testSupport.reference.TramStations;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.OldhamCentral;

public class UpcomingDates {

    private static final TramDate sunday;
    private static final TramDate saturday;
    private static final TramDate monday;

    static {
        final TramDate today = TramDate.from(TestEnv.LocalNow());

        saturday = getNextDate(DayOfWeek.SATURDAY, today);
        sunday = getNextDate(DayOfWeek.SUNDAY, today);
        monday = getNextDate(DayOfWeek.MONDAY, today);
    }

    // use helper methods that handle filtering (i.e. for Christmas) and conversion to dates
    static final int DAYS_AHEAD = 14;

    public static TramDate RochdaleLineClosure = TramDate.of(2026, 3, 22);

    public static TramDate VictoriaEndOfMarch = TramDate.of(2026,3,29);

    public static DateRange ReplacementBus1Running = DateRange.of(TramDate.of(2026,3,22),
            TramDate.of(2026,4,12));

    public static List<TramDate> MissingGreenDates = Arrays.asList(TramDate.of(2026,4,3), TramDate.of(2026,4,6));

    public static boolean hasClosure(final Station station, final TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(Station station, TramDate date, TimeRange timeRange) {
        if (hasClosure(station, date)) {
            return true;
        }
        return false;
    }

    public static boolean hasClosure(TramStations station, TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(final IdFor<Station> stationId, final TramDate date) {
        if (OldhamCentral.getId().equals(stationId)) {
            if (date.equals(RochdaleLineClosure) || date.equals(VictoriaEndOfMarch)) {
                return true;
            }
        }
        return anyClosedOnDate(date);
    }

    public static boolean anyClosedOnDate(TramDate date) {
        return false;
    }

    public static boolean validTestDate(final TramDate date) {
        return !(date.isChristmasPeriod());
    }

    public static List<TramDate> daysAhead() {
        TramDate date =  TramDate.of(TestEnv.LocalNow().toLocalDate()).plusDays(1);

        final List<TramDate> dates = new ArrayList<>();
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
        TramDate result = saturday;
        while (!validTestDate(result)) {
            result = result.plusWeeks(1);
        }
        return result;
    }

    public static TramDate nextMonday() {
        return monday;
    }


    static TramDate getNextDate(final DayOfWeek dayOfWeek, final TramDate date) {
        TramDate result = date;
        while (result.getDayOfWeek() != dayOfWeek) {
            result = result.plusDays(1);
        }
        while (!validTestDate(result)) {
            result = result.plusWeeks(1);
        }
        return result;
        //return avoidChristmasDate(result);
    }

    public static TramDate avoidChristmasDate(final TramDate startDate) {
        TramDate result = startDate;
        while (result.isChristmasPeriod()) {
            result = result.plusWeeks(1);
        }
        return result;
    }

    public static boolean isChristmasDay(TramDate date) {
        LocalDate localDate = date.toLocalDate();
        return localDate.getMonth().equals(Month.DECEMBER) && localDate.getDayOfMonth() == 25;
    }

    public static boolean isBoxingDay(TramDate date) {
        LocalDate localDate = date.toLocalDate();
        return localDate.getMonth().equals(Month.DECEMBER) && localDate.getDayOfMonth() == 26;
    }

    public static boolean hasClosure(LocationIdPair<Station> pair, TramDate date) {
        if (hasClosure(pair.getBeginId(), date)) {
            return true;
        }
        return hasClosure(pair.getEndId(), date);
    }

    public static TramDate testDay() {
        final TramDate today = TramDate.from(TestEnv.LocalNow());
        return UpcomingDates.getNextDate(DayOfWeek.THURSDAY, today);
    }
}


