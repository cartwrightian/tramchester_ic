package com.tramchester.testSupport;

import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateRanges;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.reference.TramStations;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static TramDate LateMayBankHol2025 = TramDate.of(2025, 5, 26);

    public static List<IdFor<Station>> BuryLine10MayStations = Arrays.asList(Crumpsal.getId(),
            Station.createId("9400ZZMABOW"), HeatonPark.getId(), Station.createId("9400ZZMAPWC"),
            Station.createId("9400ZZMABOB"), Whitefield.getId(), Station.createId("9400ZZMARAD"), Bury.getId());

    public static List<IdFor<Station>> BuryLine11MayStations = Arrays.asList(Whitefield.getId(),
            Station.createId("9400ZZMARAD"), Bury.getId());

    public static List<IdFor<Station>> AirportLineMayStations = Arrays.asList(TramStations.SaleWaterPark.getId(),
            Station.createId("9400ZZMANMR"), Station.createId("9400ZZMAWPK"), Station.createId("9400ZZMAMRD"),
            Station.createId("9400ZZMABAG"), Station.createId("9400ZZMARND"), Station.createId("9400ZZMAMFT"),
            Station.createId("9400ZZMABLL"), Station.createId("9400ZZMACSS"), Station.createId("9400ZZMAWYT"),
            Station.createId("9400ZZMARWD"), PeelHall.getId(), Station.createId("9400ZZMASDW"),
            ManAirport.getId());

    public static List<IdFor<Station>> RochdaleLineStations = Arrays.asList(Station.createId("9400ZZMAFRE"),
            Station.createId("9400ZZMAWWD"), OldhamKingStreet.getId(), OldhamCentral.getId(), OldhamMumps.getId(),
            Station.createId("9400ZZMADER"), ShawAndCrompton.getId(), Station.createId("9400ZZMANHY"),
            Station.createId("9400ZZMAMIL"), Station.createId("9400ZZMAKNY"), Station.createId("9400ZZMANBD"),
            RochdaleRail.getId(), Rochdale.getId());

    public static DateRanges AirportLineWorks2025 = new DateRanges(
            //DateRange.of(TramDate.of(2025,5, 11), 1),
            DateRange.of(TramDate.of(2025, 5, 18), 0)); // <- this is not on the web site

    public static DateRanges RochdaleLineWorksSummer2025 = new DateRanges(
            //DateRange.of(TramDate.of(2025, 5,3),1),
            //DateRange.of(TramDate.of(2025,5,10),1),
            DateRange.of(TramDate.of(2025,5,17),1),
            DateRange.of(TramDate.of(2025, 5, 25), 0)); // <- this is not on the website

    public static boolean hasClosure(final Station station, final TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(TramStations station, TramDate date) {
        return hasClosure(station.getId(), date);
    }

    public static boolean hasClosure(IdFor<Station> stationId, TramDate date) {
        if (RochdaleLineStations.contains(stationId)) {
            return RochdaleLineWorksSummer2025.contains(date);
        }
        if (BuryLine10MayStations.contains(stationId)) {
//            if (TramDate.of(2025,5,10).equals(date)) {
//                return true;
//            }
            if (TramDate.of(2025,5,17).equals(date)) {
                return true; // not on the website, but trams are missing from the data
            }
        }
        final IdFor<Station> radcliffe = Station.createId("9400ZZMARAD");
        if (stationId.equals(Bury.getId()) || stationId.equals(radcliffe)) {
            if (TramDate.of(2025, 5,18).equals(date)) {
                return true; // not on the website!
            }
        }
//        if (BuryLine11MayStations.contains(stationId)) {
//            if (TramDate.of(2025,5,11).equals(date)) {
//                return true;
//            }
//        }
        if (AirportLineMayStations.contains(stationId)) {
            return AirportLineWorks2025.contains(date);
        }

        return false;
    }

    public static boolean validTestDate(final TramDate date) {
//        if (date.equals(MayDay2025)) {
//            return false;
//        }
        return !(date.isChristmasPeriod());
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

    public static boolean hasClosure(LocationIdPair<Station> pair, TramDate date) {
        if (hasClosure(pair.getBeginId(),date)) {
            return true;
        }
        return hasClosure(pair.getEndId(), date);
    }
}
