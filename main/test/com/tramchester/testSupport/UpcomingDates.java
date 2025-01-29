package com.tramchester.testSupport;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
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
        TramDate today = TramDate.from(TestEnv.LocalNow());
        sunday = getNextDate(DayOfWeek.SUNDAY, today);
        saturday = getNextDate(DayOfWeek.SATURDAY, today);
        monday = getNextDate(DayOfWeek.MONDAY, today);
    }

    // use helper methods that handle filtering (i.e. for Christmas) and conversion to dates
    static final int DAYS_AHEAD = 7;

    public static DateRange MediaCityEcclesWorks2025 = DateRange.of(TramDate.of(2025,2,1),
        TramDate.of(2025, 2, 25));


//    public static IdSet<Station> PiccAshtonClosureStations = Stream.of(
//        VeloPark.getId(),
//        Piccadilly.getId(),
//        NewIslington.getId(),
//        HoltTown.getId(),
//        Etihad.getId(),
//        StringIdFor.createId("9400ZZMAELN", Station.class),
//        StringIdFor.createId("9400ZZMADRO", Station.class),
//        StringIdFor.createId("9400ZZMACLN", Station.class),
//        StringIdFor.createId("9400ZZMACEM", Station.class),
//        StringIdFor.createId("9400ZZMAAUD", Station.class),
//        StringIdFor.createId("9400ZZMAAWT", Station.class),
//        Ashton.getId(),
//        StringIdFor.createId("9400ZZMAAMO", Station.class)
//    ).collect(IdSet.idCollector());


    public static boolean hasClosure(final Station station, final TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(TramStations station, TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(IdFor<Station> stationId, TramDate date) {
        return false;
//        if (date.equals(PiccAshtonImprovementWorks)) {
//            return PiccAshtonClosureStations.contains(stationId);
//        } else {
//            return false;
//        }
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
