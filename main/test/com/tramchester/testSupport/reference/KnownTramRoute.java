package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.Set;

/**
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRoute {

//    ReplacementBusOldhamMumpsRochsdale2178("Pink Line Replacement Bus Oldham Mumps - Rochdale", "Oldham Mumps - Rochdale","2178"),
//    ReplacementBusPiccadillyVictoria("Piccadilly - Victoria Replacement Bus", "Piccadilly - Victoria", "844"),

//    old_PiccadillyAltrincham("Purple Line", "Etihad Campus - Piccadilly - Altrincham", "2173"),
//    old_BuryManchesterAltrincham("Green Line", "Bury - Manchester - Altrincham", "841"),
//    old_EcclesManchesterAshtonUnderLyne("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "2119"),
//    old_PiccadillyBury("Yellow Line", "Piccadilly - Bury", "844"),
//    old_RochdaleShawandCromptonManchesterEastDidisbury("Pink Line", "Rochdale - Manchester - East Didsbury", "845"),
//    old_VictoriaWythenshaweManchesterAirport("Navy Line", "Victoria - Wythenshawe - Manchester Airport", "2120"),
//    old_CornbrookTheTraffordCentre("Red Line", "Deansgate-Castlefield - The Trafford Centre", "849"),


    PiccadillyBury("Yellow Line", "Piccadilly - Bury", "2197"),
    PiccadillyAltrincham("Purple Line", "Etihad Campus - Piccadilly - Altrincham", "2195"),
    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line", "Rochdale - Manchester - East Didsbury", "2121"),
    BuryManchesterAltrincham("Green Line", "Bury - Manchester - Altrincham", "2192"),
    VictoriaWythenshaweManchesterAirport("Navy Line", "Victoria - Wythenshawe - Manchester Airport", "2193"),
    CornbrookTheTraffordCentre("Red Line", "Deansgate-Castlefield - The Trafford Centre", "2196"),
    EcclesManchesterAshtonUnderLyne("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "2191")

    ;

    private final String shortName;
    private final String longName;
    private final IdFor<Route> id;

    public static Set<KnownTramRoute> getFor(final TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.noneOf(KnownTramRoute.class);

        routes.add(VictoriaWythenshaweManchesterAirport);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(EcclesManchesterAshtonUnderLyne);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);
        routes.add(BuryManchesterAltrincham);
        routes.add(PiccadillyAltrincham);
        routes.add(PiccadillyBury);

        return routes;
    }

    KnownTramRoute(String shortName, String longName, String id) {
        this.longName = longName;
        this.shortName = shortName;
        this.id = Route.createId(id);
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
