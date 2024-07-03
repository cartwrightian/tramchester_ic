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

    PiccadillyAltrincham("Purple Line", "Piccadilly - Altrincham", "2122"),

    BuryManchesterAltrincham("Green Line", "Bury - Manchester - Altrincham", "2127"),
    EcclesManchesterAshtonUnderLyne("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "2119"),
    PiccadillyBury("Yellow Line", "Piccadilly - Bury", "2125"),
    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line",
            "Rochdale - Manchester - East Didsbury", "2121"),
    VictoriaWythenshaweManchesterAirport("Navy Line", "Victoria - Wythenshawe - Manchester Airport", "2120"),
    CornbrookTheTraffordCentre("Red Line", "Deansgate-Castlefield - The Trafford Centre", "2126");

    private final String shortName;
    private final String longName;
    private final IdFor<Route> id;

    public static Set<KnownTramRoute> getFor(final TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.noneOf(KnownTramRoute.class);
        boolean sunday = date.getDayOfWeek().equals(DayOfWeek.SUNDAY);

        routes.add(VictoriaWythenshaweManchesterAirport);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(EcclesManchesterAshtonUnderLyne);

        if (!londonRoadTrackReplacement(date)) {
            routes.add(PiccadillyBury);
            routes.add(PiccadillyAltrincham);
        }

        if (!sunday) {
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

    public static boolean londonRoadTrackReplacement(TramDate date) {
        DateRange range = new DateRange(TramDate.of(2024, 6, 22), TramDate.of(2024, 7, 9));
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
