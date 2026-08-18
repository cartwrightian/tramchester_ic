package com.tramchester.testSupport;

import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.integration.repository.StopCallRepositoryTest;
import com.tramchester.testSupport.reference.TramStations;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.domain.dates.TramDate.of;
import static com.tramchester.integration.repository.StopCallRepositoryTest.DerkerToRochdale;

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

    ///  NOTES
    /// See StopCallRepositoryTest for way of id'ing a set of stops on a particular line

    // use helper methods that handle filtering (i.e. for Christmas) and conversion to dates
    static final int DAYS_AHEAD = 14;

//    public static DateRange rochdaleLinePhase1 = DateRange.of(TramDate.of(2026, 8, 15),
//            TramDate.of(2026, 8, 16));

    // TODO official end date is 28th August
    public static DateRange rochdaleLinePhase2 = DateRange.of(TramDate.of(2026, 8, 17),
            TramDate.of(2026, 9, 1));

    public static DateRange rochdaleLine2026Unpublished = DateRange.of(TramDate.of(2026, 8, 22),
            TramDate.of(2026, 8, 23));

    public static TramDate summerBankHol2026 = TramDate.of(2026, 8, 31);

    public static boolean hasClosure(final IdFor<Station> stationId, final TramDate date) {
        // Add closures to the TimeRange version
        return hasClosure(stationId, date, TimeRange.AllDay());
    }

    public static boolean hasClosure(final IdFor<Station> stationId, final TramDate date, final TimeRange timeRange) {
        if (rochdaleLinePhase2.contains(date)) {
            if (DerkerToRochdale.contains(stationId) || TramStations.OldhamMumps.matches(stationId)) {
                return true;
            }
        }
        if (rochdaleLine2026Unpublished.contains(date)) {
            if (StopCallRepositoryTest.getMonsallToOldhamCentral().contains(stationId)) {
                return true;
            }
            if (TramStations.ExchangeSquare.matches(stationId)) {
                return true;
            }
        }
        // TODO Not seeing info on web site about closures, but routes are missing this date (as of 17/8)
        if (summerBankHol2026.equals(date)) {
            return true;
        }
        return false;
    }

    public static boolean notChristmasPeriod(final TramDate date) {
        return !(date.isChristmasPeriod());
    }

    public static List<TramDate> daysAhead() {
        TramDate date =  of(TestEnv.LocalNow().toLocalDate()).plusDays(1);

        final List<TramDate> dates = new ArrayList<>();
        while (dates.size() <= DAYS_AHEAD) {
            if (notChristmasPeriod(date)) {
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
        while (result.isChristmasPeriod()) {
            result = result.plusWeeks(1);
        }
        return result;
    }

    public static TramDate nextSaturday() {
        TramDate result = saturday;
        while (result.isChristmasPeriod()) {
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
        while (result.isChristmasPeriod()) {
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


