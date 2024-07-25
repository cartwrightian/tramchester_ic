package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/**
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRoute {

    // present in data but unused?
    //ReplacementBusOldhamMumpsRochsdaleA("Replacement Bus Oldham Mumps - Rochdale", "Oldham Mumps - Rochdale","2178"),
    ReplacementBusOldhamMumpsRochsdaleB("Replacement Bus Oldham Mumps - Rochdale", "Oldham Mumps - Rochdale","2177"),

    PiccadillyAltrincham("Purple Line", "Etihad Campus - Piccadilly - Altrincham", "2173"),
    BuryManchesterAltrincham("Green Line", "Bury - Manchester - Altrincham", "841"),
    EcclesManchesterAshtonUnderLyne("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "2119"),
    PiccadillyBury("Yellow Line", "Piccadilly - Bury", "844"),
    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line", "Oldham Mumps - Manchester - East Didsbury", "845"),
    VictoriaWythenshaweManchesterAirport("Navy Line", "Victoria - Wythenshawe - Manchester Airport", "2120"),
    CornbrookTheTraffordCentre("Red Line", "Deansgate-Castlefield - The Trafford Centre", "849");

    private final String shortName;
    private final String longName;
    private final IdFor<Route> id;

    public static Set<KnownTramRoute> getFor(final TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.noneOf(KnownTramRoute.class);
        boolean sunday = date.getDayOfWeek().equals(DayOfWeek.SUNDAY);

        routes.add(VictoriaWythenshaweManchesterAirport);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(EcclesManchesterAshtonUnderLyne);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);

        if (!shudehillMarketStreetClosure(date)) {
            routes.add(PiccadillyAltrincham);
            routes.add(PiccadillyBury);
        }

        if (replacementBusForLandslide(date)) {
            //routes.add(ReplacementBusOldhamMumpsRochsdaleA);
            routes.add(ReplacementBusOldhamMumpsRochsdaleB);
        }

        if (!sunday) {
            // not documented anywhere, but does not appear any trams on this route on Sundays
            routes.add(BuryManchesterAltrincham);
        }
        
        return routes;
    }

    private static boolean replacementBusForLandslide(TramDate date) {
        final DateRange range = new DateRange(TramDate.of(2024, 7, 24), TramDate.of(2024, 10, 1));
        return range.contains(date);
    }

    KnownTramRoute(String shortName, String longName, String id) {
        this.longName = longName;
        this.shortName = shortName;
        this.id = Route.createId(id);
    }

    public static boolean shudehillMarketStreetClosure(TramDate date) {
        final DateRange range = new DateRange(TramDate.of(2024, 7, 24), TramDate.of(2024, 8, 19));
        return range.contains(date);
    }

    public static int numberOn(TramDate date) {
        return getFor(date).size();
    }

    public TransportMode mode() { return TransportMode.Tram; }

    /**
     * use with care for tfgm, is duplicated and needs to be combined with RouteDirection
     * @return short name for a route
     */
    public String shortName() {
        return shortName;
    }

    public String longName() {
        return longName;
    }

    public IdFor<Route> getId() {
        return id;
    }
}
