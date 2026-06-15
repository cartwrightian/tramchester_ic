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
import static com.tramchester.testSupport.UpcomingDates.victoriaClosedUntil10amJuly2026;
import static com.tramchester.testSupport.UpcomingDates.victoriaClosedUntil10amJune2026;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Replacement Buses, come and go
//    ReplacementBusOne(BusOne, "Replacement Bus 1", "3080", ReplacementBusEaster2026),
//    ReplacementBusTwo(BusTwo, "Replacement Bus 2", "2736", ReplacementBusEaster2026),
//    ReplacementBusThree(BusThree, "Replacement Bus 3", "2361", ReplacementBusEaster2026),
//    ReplacementBusFour(BusFour, "Replacement Bus 4", "3229", ReplacementBusEaster2026),
//    ReplacementBusFive(BusFive, "Replacement Bus 5", "2177", ReplacementBusEaster2026),
//    ReplacementBusBlue(BusBlue,"Replacement Bus Blue", "3224", TramDate.of(2026,4,25)),

    ReplacementBusPicVic(BusPicVic, "Piccadilly - Victoria", "1950", victoriaClosedUntil10amJune2026),

    // Blue
    Blue1(Blue, "Eccles - Ashton Under Lyne", "3217", currentValidityDate),
    Blue2(Blue, "Eccles - Manchester - Ashton Under Lyne", "843", cutoverDateA),
    Blue3(Blue, "Eccles - Ashton Under Lyne", "3217", cutoverDateB),
    Blue4(Blue, "Eccles - Manchester - Ashton Under Lyne", "1788", victoriaClosedUntil10amJune2026),
    Blue5(Blue, "Eccles - Ashton Under Lyne", "3217", victoriaClosedUntil10amJune2026.plusDays(1)),
    Blue6(Blue, "Eccles - Manchester - Ashton Under Lyne", "1788", victoriaClosedUntil10amJuly2026),
    Blue7(Blue, "Eccles - Ashton Under Lyne", "3217", victoriaClosedUntil10amJuly2026.plusDays(1)),

    // Green
    Green1(Green, "Bury - Manchester - Altrincham", "3218", currentValidityDate),
    Green2(Green, "Altrincham - Bury", "3262", cutoverDateA),
    Green3(Green, "Bury - Manchester - Altrincham", "3218", cutoverDateB),

    // Navy
    Navy1(Navy, "Victoria - Manchester Airport", "3219", currentValidityDate),
    Navy2(Navy, "Manchester Airport - Victoria", "3264", cutoverDateA),
    Navy3(Navy, "Victoria - Manchester Airport", "3219", cutoverDateB),
    Navy4(Navy, "Manchester Airport - Victoria", "3273", victoriaClosedUntil10amJune2026),
    Navy5(Navy, "Victoria - Manchester Airport", "3219", victoriaClosedUntil10amJune2026.plusDays(1)),
    Navy6(Navy, "Manchester Airport - Victoria", "3273", victoriaClosedUntil10amJuly2026),
    Navy7(Navy, "Victoria - Manchester Airport", "3219", victoriaClosedUntil10amJuly2026.plusDays(1)),

    // Pink
    Pink1(Pink, "Rochdale - East Didsbury", "3220", currentValidityDate),
    Pink2(Pink, "East Didsbury - Rochdale", "3263", cutoverDateA),
    Pink3(Pink, "Rochdale - East Didsbury", "3220", cutoverDateB),
    Pink4(Pink, "East Didsbury - Rochdale", "3272", victoriaClosedUntil10amJune2026),
    Pink5(Pink, "Rochdale - East Didsbury", "3220", victoriaClosedUntil10amJune2026.plusDays(1)),
    Pink6(Pink, "East Didsbury - Rochdale", "3272", victoriaClosedUntil10amJuly2026),
    Pink7(Pink, "Rochdale - East Didsbury", "3220", victoriaClosedUntil10amJuly2026.plusDays(1)),

    // Purple
    Purple1(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", currentValidityDate),
    Purple2(Purple, "Altrincham - Etihad Campus", "3266", cutoverDateA),
    Purple3(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", cutoverDateB),
    Purple4(Purple, "Altrincham - Etihad Campus", "3275", victoriaClosedUntil10amJune2026),
    Purple5(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", victoriaClosedUntil10amJune2026.plusDays(1)),
    Purple6(Purple, "Altrincham - Etihad Campus", "3275", victoriaClosedUntil10amJuly2026),
    Purple7(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", victoriaClosedUntil10amJuly2026.plusDays(1)),

    // Red
    Red1(Red, "Deansgate Castlefield - The Trafford Centre", "3222", currentValidityDate),
    Red2(Red, "Trafford Centre - Crumpsall", "3265", cutoverDateA),
    Red3(Red, "Deansgate Castlefield - The Trafford Centre", "3222", cutoverDateB),
    Red4(Red, "Trafford Centre - Crumpsall", "3274", victoriaClosedUntil10amJune2026),
    Red5(Red, "Deansgate Castlefield - The Trafford Centre", "3222", victoriaClosedUntil10amJune2026.plusDays(1)),
    Red6(Red, "Trafford Centre - Crumpsall", "3274", victoriaClosedUntil10amJuly2026),
    Red7(Red, "Deansgate Castlefield - The Trafford Centre", "3222", victoriaClosedUntil10amJuly2026.plusDays(1)),

    // Yellow
    Yellow1(Yellow, "Piccadilly - Bury", "3223", currentValidityDate),
    Yellow2(Yellow, "Piccadilly - Bury", "844", cutoverDateA),
    Yellow3(Yellow, "Piccadilly - Bury", "3223", cutoverDateB),
    Yellow4(Yellow, "Piccadilly - Bury", "844", victoriaClosedUntil10amJune2026),
    Yellow5(Yellow, "Piccadilly - Bury", "3223", victoriaClosedUntil10amJune2026.plusDays(1)),
    Yellow6(Yellow, "Piccadilly - Bury", "844", victoriaClosedUntil10amJuly2026),
    Yellow7(Yellow, "Piccadilly - Bury", "3223", victoriaClosedUntil10amJuly2026.plusDays(1));

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

}
