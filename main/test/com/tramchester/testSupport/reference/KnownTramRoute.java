package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.reference.RouteDirection.Outbound;
import static java.lang.String.format;

/**
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRoute {

    //AltrinchamPiccadilly("Purple Line", Inbound, "Altrincham - Piccadilly"),
    PiccadillyAltrincham("Purple Line", Outbound, "Piccadilly - Altrincham"),

    //AltrinchamManchesterBury("Green Line", Inbound, "Altrincham - Manchester - Bury"),
    BuryManchesterAltrincham("Green Line", Outbound, "Bury - Manchester - Altrincham"),

    //AshtonUnderLyneManchesterEccles("Blue Line", Inbound, "Ashton Under Lyne - Manchester - Eccles"),
    EcclesManchesterAshtonUnderLyne("Blue Line", Outbound, "Eccles - Manchester - Ashton Under Lyne"),

    //BuryPiccadilly("Yellow Line", Inbound,"Bury - Piccadilly"),
    PiccadillyBury("Yellow Line", Outbound, "Piccadilly - Bury"),

    //EastDidisburyManchesterShawandCromptonRochdale("Pink Line", Inbound, "East Didsbury - Manchester - Shaw and Crompton - Rochdale"),
    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line", Outbound, "Rochdale - Shaw and Crompton - Manchester - East Didsbury"),

    //ManchesterAirportWythenshaweVictoria("Navy Line", Inbound, "Manchester Airport - Wythenshawe - Victoria"),
    VictoriaWythenshaweManchesterAirport("Navy Line", Outbound, "Victoria - Wythenshawe - Manchester Airport"),

    //TheTraffordCentreCornbrook("Red Line", Inbound, "The Trafford Centre - Cornbrook"),
    CornbrookTheTraffordCentre("Red Line", Outbound, "Cornbrook - The Trafford Centre");

    private final IdFor<Route> fakeId;
    private final RouteDirection direction;
    private final String shortName;
    private final String longName;

    // tram route merge workaround, TODO inline these at some point?
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
    @Deprecated


    public static Set<KnownTramRoute> getFor(TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.noneOf(KnownTramRoute.class);

        routes.add(VictoriaWythenshaweManchesterAirport);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);
        routes.add(EcclesManchesterAshtonUnderLyne);
        routes.add(PiccadillyBury);
        routes.add(PiccadillyAltrincham);

        if (!date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            // not docuemented anywhere, but does not appear any trams on this route on Sundays
            routes.add(BuryManchesterAltrincham);
        }

        return routes;
    }

    KnownTramRoute(String shortName, RouteDirection direction, String longName) {
        this.longName = longName;
        this.direction = direction;
        this.shortName = shortName;

        // new format for IDs METLRED:I:xxxxxx
        String idSuffix;
        if (shortName.contains("Replacement") || shortName.contains("Replaement")) { // yep, typo in the source data
            idSuffix = getSuffixFor(shortName);
        } else {
            int endIndex = Math.min(shortName.length(), 4);
            idSuffix = shortName.toUpperCase().substring(0, endIndex).trim();
        }
        this.fakeId = createId(format("METL%s%sCURRENT", idSuffix, direction.getSuffix()));

    }

    public static int numberOn(TramDate date) {
        return getFor(date).size();
    }

    private String getSuffixFor(String shortName) {
        return switch (shortName) {
            case "Blue Line Bus Replacement" -> "MLDP";
            default -> throw new RuntimeException("Unexpected replacement service short name" + shortName);
        };
    }

    private IdFor<Route> createId(String stationId) {
        return Route.createId(stationId);
    }

    // no longer used since tfgm data format changes

//    public RouteDirection direction() {
//        return direction;
//    }

    public TransportMode mode() { return TransportMode.Tram; }

    /**
     * use with care for tfgm, is duplicated and needs to be combined with RouteDirection
     * @return short name for a route
     */
    public String shortName() {
        return shortName;
    }

    public IdFor<Route> getFakeId() {
        return fakeId;
    }

    public String longName() {
        return longName;
    }

}
