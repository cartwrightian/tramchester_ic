package com.tramchester.testSupport;

import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
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

    static {
        final TramDate today = TramDate.from(TestEnv.LocalNow());

        saturday = getNextDate(DayOfWeek.SATURDAY, today);
        sunday = getNextDate(DayOfWeek.SUNDAY, today);
        monday = getNextDate(DayOfWeek.MONDAY, today);
    }

    // use helper methods that handle filtering (i.e. for Christmas) and conversion to dates
    static final int DAYS_AHEAD = 14;

    public static DateRange AshtonLineLateApril2026 = DateRange.of(TramDate.of(2026,4,25), 1);

    public static TramDate earlyMayBankHold = TramDate.of(2026, 5,4);
    public static TramDate lateMayBankHold = TramDate.of(2026, 5,25);

    public static TramDate victoriaWorkEarlyMay2026 = TramDate.of(2026, 5, 3);
    public static TramDate victoriaAndRochdaleLineMay2026 = TramDate.of(2026, 5, 10);

    public static boolean hasClosure(final Station station, final TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(TramStations station, TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(HasId<Station> station, TramDate date, TimeRange timeRange) {
        return hasClosure(station.getId(), date, timeRange);
    }

    public static boolean hasClosure(IdFor<Station> stationId, TramDate date, TimeRange timeRange) {
        if (hasClosure(stationId, date)) {
            return true;
        }
        if (victoriaWorkEarlyMay2026.equals(date)) {
            TimeRange closure = TimeRange.of(TramTime.of(4,0), TramTime.of(10,0));
            if (closure.anyOverlap(timeRange)) {
                return true;
            }
        }
        if (victoriaAndRochdaleLineMay2026.equals(date)) {
            TimeRange closure = TimeRange.of(TramTime.of(4,0), TramTime.of(11,0));
            if (closure.anyOverlap(timeRange)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasClosure(final IdFor<Station> stationId, final TramDate date) {
        if (TramStations.Shudehill.getId().equals(stationId) || TramStations.MarketStreet.getId().equals(stationId)) {
            return date.isEqual(TramDate.of(2026,5,3));
        }

        return anyClosedOnDate(date);
    }

    public static boolean anyClosedOnDate(TramDate date) {
//        if (Easter2026Works.contains(date)) {
//            return true;
//        }
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


