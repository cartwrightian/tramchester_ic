package com.tramchester.testSupport;

import com.tramchester.domain.LocationIdPair;
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

import static com.tramchester.testSupport.reference.TramStations.*;

public class UpcomingDates {

    private static final TramDate sunday;
    private static final TramDate saturday;
    private static final TramDate monday;

    static {
        TramDate today = TramDate.from(TestEnv.LocalNow());
        sunday = getNextDate(DayOfWeek.SUNDAY, today);
        saturday = getNextDate(DayOfWeek.SATURDAY, today);
        monday = getNextDate(DayOfWeek.MONDAY, today);
    }

    // use helper methods that handle filtering (i.e. for Christmas) and conversion to dates
    static final int DAYS_AHEAD = 14;

    public static TramDate VictoriaAndRochdaleLineWorks = TramDate.of(2025,11,16);
    public static TramDate VictoriaNov2025 = TramDate.of(2025, 11,23);
    public static TramDate ExchangeSquareUnpublished = TramDate.of(2025, 11, 9);

    public static boolean hasClosure(final Station station, final TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(Station station, TramDate date, TimeRange timeRange) {

        TimeRange exchangeSquareNoTrams = TimeRange.of(TramTime.of(6, 25), TramTime.of(7, 35));
        if (ExchangeSquare.getId().equals(station.getId())) {
            if (ExchangeSquareUnpublished.equals(date) && exchangeSquareNoTrams.anyOverlap(timeRange)) {
                return true;
            } else {
                return false;
            }
        }

        if (hasClosure(station, date)) {
            return true;
        }
        return false;
    }

    public static boolean hasClosure(TramStations station, TramDate date) {

        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(IdFor<Station> stationId, TramDate date) {
        if (VictoriaAndRochdaleLineWorks.equals(date)) {
            if (stationId.equals(MarketStreet.getId()) || stationId.equals(OldhamCentral.getId())) {
                return true;
            }
        }
        return anyClosedOnDate(date);
    }

    public static boolean anyClosedOnDate(TramDate date) {
//        if (TraffordBar2025.contains(date)) {
//            return true;
//        }
        return false;
    }

    public static boolean validTestDate(final TramDate date) {
        return !(date.isChristmasPeriod());
    }

    public static List<TramDate> daysAhead() {
        TramDate date = TramDate.of(TestEnv.LocalNow().toLocalDate()).plusDays(1);

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
}


