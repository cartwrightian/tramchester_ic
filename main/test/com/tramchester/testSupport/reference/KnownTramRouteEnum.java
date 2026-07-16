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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;
import static com.tramchester.testSupport.UpcomingDates.summer2026MajorClosure;
import static com.tramchester.testSupport.UpcomingDates.summerClosureFirstSunday;
import static com.tramchester.testSupport.reference.KnownTramRoute.cutoverDate;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Blue
    //Blue1(Blue, "Eccles - Ashton Under Lyne", "3217", cutoverDate),
    Blue2(Blue, "Eccles - Ashton Under Lyne", "3278", Constants.dayBeforeSummer2026Closures),

    // Green
    Green1(Green, "Bury - Manchester - Altrincham", "", cutoverDate),

    // Navy
    //Navy1(Navy, "Victoria - Manchester Airport", "3219", cutoverDate),
    Navy2(Navy, "Victoria - Manchester Airport", "3280", Constants.dayBeforeSummer2026Closures),
    Navy3(Navy, "Manchester Airport - Victoria", "3287", summerClosureFirstSunday),
    Navy4(Navy, "Victoria - Manchester Airport", "3300", summerClosureFirstSunday.plusDays(1)),
    Navy5(Navy, "Manchester Airport - Victoria", "3314", TramDate.of(2026,7,26)),
    Navy6(Navy, "Victoria - Manchester Airport", "3300", TramDate.of(2026,7,27)),

    // Pink
    //Pink1(Pink, "Rochdale - East Didsbury", "3220", cutoverDate),
    Pink2(Pink, "Rochdale - East Didsbury", "3281", Constants.dayBeforeSummer2026Closures),
    Pink3(Pink, "East Didsbury - Rochdale" , "3286", summerClosureFirstSunday),
    Pink4(Pink, "Rochdale - East Didsbury" , "3301", summerClosureFirstSunday.plusDays(1)),
    Pink5(Pink, "East Didsbury - Rochdale" , "3311", TramDate.of(2026,7,26)),
    Pink6(Pink, "Rochdale - East Didsbury" , "3301", TramDate.of(2026,7,27)),

    // Purple
    //Purple3(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", cutoverDate),
    Purple8(Purple, "Etihad Campus - Piccadilly - Altrincham", "3282", Constants.dayBeforeSummer2026Closures),

    // Red
    Red8(Red, "Deansgate-Castlefield - The Trafford Centr", "3283", Constants.dayBeforeSummer2026Closures),

    // Yellow
    Yellow2(Yellow, "Piccadilly - Bury", "3284", Constants.dayBeforeSummer2026Closures),
    Yellow3(Yellow, "Piccadilly - Bury", "844", summerClosureFirstSunday),
    Yellow4(Yellow, "Piccadilly - Bury", "3302", summerClosureFirstSunday.plusDays(1)),
    Yellow5(Yellow, "Piccadilly - Bury", "844", TramDate.of(2026,7,26)),
    Yellow6(Yellow, "Piccadilly - Bury", "3302", TramDate.of(2026,7,27)),

    ;

    private final TFGMRouteNames line;
    private final String longName;
    private final IdFor<Route> id;
    private final TramDate validFrom;

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom) {
        this.longName = longName;
        if (id.isEmpty()) {
            this.id = IdFor.invalid(Route.class);
        } else {
            this.id = TramRouteId.create(line, id);
        }
        this.validFrom = validFrom;
        this.line = line;
    }

    public static EnumSet<KnownTramRouteEnum> validRoutes() {
        return Arrays.stream(values()).
                filter(item -> item.getId().isValid()).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(KnownTramRouteEnum.class)));
    }

    public TramDate getValidFrom() {
        return validFrom;
    }

    @Override
    public TransportMode mode() {
        return TransportMode.Tram;
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
        return id;
    }

    @Override
    public IdForDTO dtoId() {
        return IdForDTO.createFor(id);
    }

    @Override
    public Route fake() {
        return new MutableRoute(id, line.getShortName(), longName, TestEnv.MetAgency(), TransportMode.Tram);
    }

    @Override
    public String toString() {
        return line + "["+name()+"]";
    }

    private static class Constants {
        public static final TramDate dayBeforeSummer2026Closures = summer2026MajorClosure.getStartDate().minusDays(1);
        public static TramDate summer2026Closures = summer2026MajorClosure.getStartDate();
    }
}
