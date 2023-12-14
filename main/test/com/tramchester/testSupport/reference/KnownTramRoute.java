package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
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

    PiccadillyAltrincham("Purple Line", "Piccadilly - Altrincham", "842"),

    BuryManchesterAltrincham("Green Line", "Crumpsall - Manchester - Altrincham", "841"),

    EcclesManchesterAshtonUnderLyne("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "843"),

    PiccadillyBury("Yellow Line", "Piccadilly - Bury", "844"),

    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line",
            "Rochdale - Shaw and Crompton - Manchester - East D", "845"),

    VictoriaWythenshaweManchesterAirport("Navy Line", "Victoria - Wythenshawe - Manchester Airport", "848"),

    CornbrookTheTraffordCentre("Red Line", "Deansgate Castlefield - The Trafford Centre", "849");

    private final String shortName;
    private final String longName;
    private final IdFor<Route> id;

    // tram route merge workaround, TODO inline these at some point
    @Deprecated
    public static final KnownTramRoute AltrinchamPiccadilly = KnownTramRoute.PiccadillyAltrincham;
    @Deprecated
    public static final KnownTramRoute AltrinchamManchesterBury = KnownTramRoute.BuryManchesterAltrincham;
    @Deprecated
    public static final KnownTramRoute AshtonUnderLyneManchesterEccles = KnownTramRoute.EcclesManchesterAshtonUnderLyne;
    @Deprecated
    public static final KnownTramRoute BuryPiccadilly = KnownTramRoute.PiccadillyBury;
    @Deprecated
    public static final KnownTramRoute EastDidisburyManchesterShawandCromptonRochdale = KnownTramRoute.RochdaleShawandCromptonManchesterEastDidisbury;
    @Deprecated
    public static final KnownTramRoute ManchesterAirportWythenshaweVictoria = KnownTramRoute.VictoriaWythenshaweManchesterAirport;
    @Deprecated
    public static final KnownTramRoute TheTraffordCentreCornbrook = KnownTramRoute.CornbrookTheTraffordCentre;

    public static Set<KnownTramRoute> getFor(TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.noneOf(KnownTramRoute.class);

        routes.add(VictoriaWythenshaweManchesterAirport);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);
        routes.add(EcclesManchesterAshtonUnderLyne);
        routes.add(PiccadillyBury);
        routes.add(PiccadillyAltrincham);

        if (!date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            // not documented anywhere, but does not appear any trams on this route on Sundays
            routes.add(BuryManchesterAltrincham);
        }

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
