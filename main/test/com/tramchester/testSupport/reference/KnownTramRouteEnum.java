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

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;
import static com.tramchester.testSupport.UpcomingDates.summer2026MajorClosure;
import static com.tramchester.testSupport.UpcomingDates.summerClosureFirstSunday;
import static com.tramchester.testSupport.reference.KnownTramRoute.MISSING_ROUTE_ID_PREFIX;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Blue
    Blue1(Blue, "Eccles - Ashton Under Lyne", "xxx2", summerClosureFirstSunday),
    Blue2(Blue, "Eccles - Ashton Under Lyne", "3217", Constants.reopenSummer2026),

    // Green
    Green1(Green, "Bury - Manchester - Altrincham", "3218", Constants.reopenSummer2026),

    // Navy
    Navy4(Navy, "Victoria - Manchester Airport", "3300", summerClosureFirstSunday.plusDays(1)),
    Navy5(Navy, "Manchester Airport - Victoria", "3314", TramDate.of(2026,7,26), true),
    Navy7(Navy, "Manchester Airport - Victoria", "3287", Constants.summerClosures2026EndDate, true),
    Navy8(Navy, "Victoria - Manchester Airport", "3219", Constants.reopenSummer2026),
    Navy9(Navy, "Manchester Airport - Victoria", "3219", TramDate.of(2026,8,9), true),
    Navy10(Navy, "Manchester Airport - Victoria", "3219", TramDate.of(2026,8,16), true),

    // Pink
    Pink4(Pink, "Rochdale - East Didsbury" , "3301", summerClosureFirstSunday.plusDays(1)),
    Pink5(Pink, "East Didsbury - Rochdale" , "3311", TramDate.of(2026,7,26), true),
    Pink7(Pink, "East Didsbury - Rochdale" , "3286", Constants.summerClosures2026EndDate, true),
    Pink8(Pink, "Rochdale - East Didsbury" , "3220", Constants.reopenSummer2026),
    Pink9(Pink, "Rochdale - East Didsbury" , "3220", TramDate.of(2026,8,9), true),
    Pink10(Pink, "Rochdale - East Didsbury" , "3220", TramDate.of(2026,8,16), true),

    // Purple
    Purple8(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", Constants.reopenSummer2026),

    // Red
    Red1(Red, "Deansgate Castlefield - The Trafford Centre", "xxx1", summerClosureFirstSunday),
    Red2(Red, "Deansgate Castlefield - The Trafford Centre", "3222", Constants.reopenSummer2026),

    // Yellow
    Yellow4(Yellow, "Piccadilly - Bury", "3302", summerClosureFirstSunday.plusDays(1)),
    Yellow5(Yellow, "Piccadilly - Bury", "844", TramDate.of(2026,7,26), true),
    Yellow7(Yellow, "Piccadilly - Bury", "844", Constants.summerClosures2026EndDate, true),
    Yellow8(Yellow, "Piccadilly - Bury", "3223", Constants.reopenSummer2026),
    Yellow9(Yellow, "Piccadilly - Bury" , "3223", TramDate.of(2026,8,9), true),
    Yellow10(Yellow, "Piccadilly - Bury" , "3223", TramDate.of(2026,8,16), true)
    ;

    private final TFGMRouteNames line;
    private final String longName;
    private final String id;
    private final TramDate validFrom;
    private final boolean sundayOnly;

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom) {
        this(line, longName, id, validFrom, false);
    }

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom, boolean sundayOnly) {
        this.longName = longName;
        this.validFrom = validFrom;
        this.line = line;
        this.id = id;
        if (sundayOnly && validFrom.getDayOfWeek()!= DayOfWeek.SUNDAY) {
            throw new RuntimeException("Line " + line + " not a sunday " + validFrom);
        }
        this.sundayOnly = sundayOnly;
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

    public boolean sundayOnly() {
        return sundayOnly;
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
        public static TramDate summerClosures2026EndDate = summer2026MajorClosure.getEndDate();
        public static TramDate reopenSummer2026 = summerClosures2026EndDate.plusDays(1);
    }
}
