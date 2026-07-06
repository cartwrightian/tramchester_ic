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
import static com.tramchester.testSupport.reference.KnownTramRoute.cutoverDate;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Replacement Buses, come and go

    BusAltrinchamPiccadilly(AltrinchamPiccadilly, "Altrincham to Piccadilly Station", "3285", Constants.summer2026Closures),
    BusPiccadillyAltrincham(AltrinchamPiccadilly, "Piccadilly Station - Altrincham", "3292", Constants.summer2026Closures),

    BusVictoriaRochsdale(RochsdaleVictoria, "Victoria - Rochdale Town Centre", "3294", Constants.dayBeforeSummer2026Closures),
    BusPiccadillyTraffordCentre(TraffordCentrePiccadilly, "Piccadilly Station - The Trafford Centre", "3291", Constants.summer2026Closures),

    BusPiccadillyChorltonA(ChorltonPiccadilly, "Piccadilly Station - Chorlton", "3293", Constants.summer2026Closures),
    BusPiccadillyChorltonB(ChorltonPiccadilly, "Piccadilly Station - Chorlton", "3288", Constants.summer2026Closures),

    // Blue
    Blue3(Blue, "Eccles - Ashton Under Lyne", "3217", cutoverDate),
    Blue8(Blue, "Eccles - Ashton Under Lyne", "3278", Constants.dayBeforeSummer2026Closures),

    // Green
    Green3(Green, "Bury - Manchester - Altrincham", "3218", cutoverDate),

    // Navy
    Navy3(Navy, "Victoria - Manchester Airport", "3219", cutoverDate),
    Navy8(Navy, "Victoria - Manchester Airport", "3280", Constants.dayBeforeSummer2026Closures),

    // Pink
    Pink3(Pink, "Rochdale - East Didsbury", "3220", cutoverDate),
    Pink8(Pink, "Rochdale - East Didsbury", "3281", Constants.dayBeforeSummer2026Closures),

    // Purple
    Purple3(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", cutoverDate),
    Purple8(Purple, "Etihad Campus - Piccadilly - Altrincham", "3282", Constants.dayBeforeSummer2026Closures),

    // Red
    Red3(Red, "Deansgate Castlefield - The Trafford Centre", "3222", cutoverDate),
    Red8(Red, "Deansgate-Castlefield - The Trafford Centr", "3283", Constants.dayBeforeSummer2026Closures),

    // Yellow
    Yellow3(Yellow, "Piccadilly - Bury", "3223", cutoverDate),
    Yellow8(Yellow, "Piccadilly - Bury", "3284", Constants.dayBeforeSummer2026Closures);

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
