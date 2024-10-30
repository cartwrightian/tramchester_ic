package com.tramchester.testSupport;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.reference.TramStations;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Rochdale;

public class UpcomingDates {

    private static final TramDate sunday;
    private static final TramDate saturday;
    private static final TramDate monday;

//    private static final Set<DateRange> closures = new HashSet<>();

    // use helper methods that handle filtering (i.e. for Christmas) and conversion to dates
    static final int DAYS_AHEAD = 7;

    // the official dates seem wrong, or the published timetable is wrong....meant to finish 31/10 but no trams in
    // time table until 8/11
    private static final DateRange rochdaleLineWorks = DateRange.of(TramDate.of(2024,10,19),
            TramDate.of(2024,11,7));

   public static final DateRange TfgmDataError = DateRange.of(TramDate.of(2024,11,4), TramDate.of(2024,11,5));

    private static final TramDate deansgateWorks27thOctober = TramDate.of(2024,10,27);

    public static final TramDate fullNetworkCloseDown = TramDate.of(2024,11,3);

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
        if (stationId.equals(Rochdale.getId())) {
            return rochdaleLineWorks.contains(date);
        }
        if (stationId.equals(Bury.getId())) {
            return date.equals(fullNetworkCloseDown);
        }
        return false;
    }

    public static boolean hasClosure(TramStations station, TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean validTestDate(final TramDate date) {
        if (date.equals(deansgateWorks27thOctober)) {
            return false;
        }
        if (TfgmDataError.contains(date)) {
            return false;
        }
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


}
