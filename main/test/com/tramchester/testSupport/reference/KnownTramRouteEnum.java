package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.MISSING_ROUTE_ID_PREFIX;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Blue
    Blue2(Blue, "Eccles - Ashton Under Lyne", "3217", Constants.earlyAugCutover),
    Blue3(Blue, "Eccles - Ashton-under-Lyne", "3322",TramDate.of(2026,8,9), DaysMod.sundayOnly),
    Blue4(Blue, "Eccles - Ashton-under-Lyne", "3326", Constants.manchesterTownHallStart),
    Blue5(Blue, "Eccles - Ashton-under-Lyne", "3354", Constants.midAugCutover),
    Blue6(Blue, "Eccles - Ashton-under-Lyne", "3359", Constants.midAugCutover.plusDays(1), DaysMod.sundayOnly),
    Blue7(Blue, "Eccles - Ashton-under-Lyne", "3326", Constants.midAugCutover.plusDays(2)),
//    Blue8(Blue, "Eccles - Ashton-under-Lyne", "3354", TramDate.of(2026, 8, 29), DaysMod.everySaturday),
//    Blue9(Blue, "Eccles - Ashton-under-Lyne", "3359", TramDate.of(2026, 8, 30)),
//    Blue10(Blue, "Eccles - Ashton-under-Lyne", "3326", TramDate.of(2026, 9, 1)),

    // Green
    Green1(Green, "Bury - Manchester - Altrincham", "3218", Constants.earlyAugCutover),
    Green2(Green, "Altrincham - Bury", "3325", Constants.manchesterTownHallStart),
    Green3(Green, "Altrincham - Bury", "3353", Constants.midAugCutover),
    Green4(Green, "Altrincham - Bury", "3325", Constants.midAugCutover.plusDays(2)),
    Green5(Green, "Altrincham - Bury", "3353", TramDate.of(2026, 8, 29)),
    Green6(Green, "Altrincham - Bury", "3325", TramDate.of(2026, 9, 1)),

    // Navy
    Navy3(Navy, "Victoria - Manchester Airport", "3219", Constants.earlyAugCutover),
    Navy4(Navy, "Manchester Airport - Victoria", "3315", TramDate.of(2026,8,9), DaysMod.sundayOnly),
    Navy5(Navy, "Manchester Airport - Victoria", "3316", Constants.manchesterTownHallStart),
    Navy6(Navy, "Manchester Airport - Victoria", "3356", Constants.midAugCutover),
    Navy7(Navy, "Manchester Airport - Victoria", "3361", Constants.midAugCutover.plusDays(1), DaysMod.sundayOnly),
    Navy8(Navy, "Manchester Airport - Victoria", "3316", Constants.midAugCutover.plusDays(2)),

    // Pink
    Pink3(Pink, "Rochdale - East Didsbury" , "3220", Constants.earlyAugCutover),
    Pink4(Pink, "East Didsbury - Rochdale" , "3312", TramDate.of(2026,8,9), DaysMod.sundayOnly),
    Pink5(Pink, "East Didsbury - Rochdale" , "3313", Constants.manchesterTownHallStart),
    Pink6(Pink, "East Didsbury - Rochdale" , "3355", Constants.midAugCutover),
    Pink7(Pink, "East Didsbury - Rochdale" , "3360", Constants.midAugCutover.plusDays(1), DaysMod.sundayOnly),
    Pink8(Pink, "East Didsbury - Rochdale" , "3313", Constants.midAugCutover.plusDays(2)),

    // Purple
    Purple1(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", Constants.earlyAugCutover),
    Purple2(Purple, "Altrincham - Etihad Campus", "3324", TramDate.of(2026,8,9), DaysMod.sundayOnly),
    Purple3(Purple, "Altrincham - Etihad Campus", "3328", Constants.manchesterTownHallStart),
    Purple4(Purple, "Altrincham - Etihad Campus", "3358", Constants.midAugCutover),
    Purple6(Purple, "Altrincham - Etihad Campus", "3363", Constants.midAugCutover.plusDays(1), DaysMod.sundayOnly),
    Purple7(Purple, "Altrincham - Etihad Campus", "3328", Constants.midAugCutover.plusDays(2)),

    // Red
    Red2(Red, "Deansgate Castlefield - The Trafford Centre", "3222", Constants.earlyAugCutover),
    Red3(Red, "Trafford Centre - Crumpsall", "3323", TramDate.of(2026,8,9), DaysMod.sundayOnly),
    Red4(Red, "Trafford Centre - Crumpsall", "3327", Constants.manchesterTownHallStart),
    Red5(Red, "Trafford Centre - Crumpsall", "3357", Constants.midAugCutover),
    Red6(Red, "Trafford Centre - Crumpsall", "3362", Constants.midAugCutover.plusDays(1), DaysMod.sundayOnly),
    Red7(Red, "Trafford Centre - Crumpsall", "3327", Constants.midAugCutover.plusDays(2)),

    // Yellow
    Yellow3(Yellow, "Piccadilly - Bury", "3223", Constants.earlyAugCutover),
    Yellow4(Yellow, "Piccadilly - Bury" , "844", TramDate.of(2026,8,9), DaysMod.everySunday),
    Yellow5(Yellow, "Piccadilly - Bury" , "844", Constants.manchesterTownHallStart),

    ;

    private final TFGMRouteNames line;
    private final String longName;
    private final String id;
    private final TramDate validFrom;
    private final DaysMod daysMod;

    public enum DaysMod {
        sundayOnly, none, everySunday, saturdayOnly, everySaturday
    }

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom) {
        this(line, longName, id, validFrom, DaysMod.none);
    }

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom, DaysMod daysMod) {
        this.longName = longName;
        this.validFrom = validFrom;
        this.line = line;
        this.id = id;
        DayOfWeek validFromDayOfWeek = validFrom.getDayOfWeek();

        switch (daysMod) {
            case sundayOnly, everySunday -> {
                if (validFromDayOfWeek!=DayOfWeek.SUNDAY) {
                    throw new RuntimeException("Line " + line + " not a Sunday " + validFrom);
                }
            }
            case saturdayOnly, everySaturday -> {
                if (validFromDayOfWeek!=DayOfWeek.SATURDAY) {
                    throw new RuntimeException("Line " + line + " not a Saturday " + validFrom);
                }
            }
        }
        this.daysMod = daysMod;
    }

    public static EnumSet<KnownTramRouteEnum> validRoutes() {
        return Arrays.stream(values()).
                filter(item -> item.getId().isValid()).
                filter(item -> !item.id.startsWith(MISSING_ROUTE_ID_PREFIX)).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(KnownTramRouteEnum.class)));
    }

    public TramDate getValidFrom() {
        return validFrom;
    }

    @Override
    public TransportMode mode() {
        return TransportMode.Tram;
    }

    public DaysMod modifiers() {
        return daysMod;
    }

    public String longName() {
        return longName;
    }

    /**
     * @return short name for a route
     */
    @Override
    public String shortName() {
        return line.getShortName();
    }

    public TFGMRouteNames line() {
        return line;
    }

    @Override
    public IdFor<Route> getId() {
        if (id.isEmpty()) {
           return IdFor.invalid(Route.class);
        } else {
            return TramRouteId.create(line, id);
        }
    }

    @Override
    public IdForDTO dtoId() {
        return IdForDTO.createFor(getId());
    }

    @Override
    public Route fake() {
        return new MutableRoute(getId(), line.getShortName(), longName, TestEnv.MetAgency(), TransportMode.Tram);
    }

    @Override
    public String toString() {
        return line + "["+name()+"]";
    }

    private static class Constants {
        public static TramDate earlyAugCutover = TramDate.of(2026,8,10);
        public static TramDate manchesterTownHallStart = UpcomingDates.manchesterTownHall2026.getStartDate();
        public static TramDate manchesterTownHallEnd = UpcomingDates.manchesterTownHall2026.getEndDate();

        public static TramDate midAugCutover = TramDate.of(2026, 8, 22);
    }
}
