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
import static com.tramchester.testSupport.reference.KnownTramRoute.cutoverDate;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Blue
    Blue1(Blue, "Eccles - Ashton Under Lyne", "xxx2", summerClosureFirstSunday),
    Blue2(Blue, "Eccles - Ashton Under Lyne", "3217", Constants.reopenSummer2026),
    Blue3(Blue, "Eccles - Ashton-under-Lyne", "3322",TramDate.of(2026,8,9), SundayOnly.every),

    // Green
    Green1(Green, "Bury - Manchester - Altrincham", "3218", Constants.reopenSummer2026),

    // Navy
    Navy1(Navy, "Victoria - Manchester Airport", "3300", cutoverDate),
    Navy2(Navy, "Manchester Airport - Victoria", "3287", Constants.summerClosures2026EndDate, SundayOnly.yes),
    Navy3(Navy, "Victoria - Manchester Airport", "3219", Constants.reopenSummer2026),
    Navy4(Navy, "Manchester Airport - Victoria", "3315", TramDate.of(2026,8,9), SundayOnly.every),

    // Pink
    Pink1(Pink, "Rochdale - East Didsbury" , "3301", cutoverDate),
    Pink2(Pink, "East Didsbury - Rochdale" , "3286", Constants.summerClosures2026EndDate, SundayOnly.yes),
    Pink3(Pink, "Rochdale - East Didsbury" , "3220", Constants.reopenSummer2026),
    Pink4(Pink, "East Didsbury - Rochdale" , "3312", TramDate.of(2026,8,9), SundayOnly.every),

    // Purple
    Purple1(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", Constants.reopenSummer2026),
    Purple2(Purple, "Altrincham - Etihad Campus", "3324", TramDate.of(2026,8,9), SundayOnly.every),

    // Red
    Red1(Red, "Deansgate Castlefield - The Trafford Centre", "xxx1", summerClosureFirstSunday),
    Red2(Red, "Deansgate Castlefield - The Trafford Centre", "3222", Constants.reopenSummer2026),
    Red3(Red, "Trafford Centre - Crumpsall", "3323", TramDate.of(2026,8,9), SundayOnly.every),

    // Yellow
    Yellow1(Yellow, "Piccadilly - Bury", "3302", cutoverDate),
    Yellow2(Yellow, "Piccadilly - Bury", "844", Constants.summerClosures2026EndDate, SundayOnly.yes),
    Yellow3(Yellow, "Piccadilly - Bury", "3223", Constants.reopenSummer2026),
    Yellow4(Yellow, "Piccadilly - Bury" , "844", TramDate.of(2026,8,9), SundayOnly.every),
    ;

    private final TFGMRouteNames line;
    private final String longName;
    private final String id;
    private final TramDate validFrom;
    private final SundayOnly sundayOnly;

    public enum SundayOnly {
        yes, no, every
    }


    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom) {
        this(line, longName, id, validFrom, SundayOnly.no);
    }

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom, SundayOnly sundayOnly) {
        this.longName = longName;
        this.validFrom = validFrom;
        this.line = line;
        this.id = id;
        if (sundayOnly!=SundayOnly.no && validFrom.getDayOfWeek()!= DayOfWeek.SUNDAY) {
            throw new RuntimeException("Line " + line + " not a Sunday " + validFrom);
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

    public SundayOnly sundayOnly() {
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
