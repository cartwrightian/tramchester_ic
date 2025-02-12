package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Replacement buses
    BusEcclesToMediaCity("Replacement Bus 1", "Eccles - Media City", "2749"),
    BusEcclesToMediaCity_new("Replacement Bus 1", "Eccles - Media City", "2757"),

    // Blue
    EcclesAshton("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "2119"),
    EcclesAshton_new("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "2750"),

    // Green
    BuryManchesterAltrincham("Green Line", "Bury - Manchester - Altrincham", "841"),
    // Navy
    DeansgateCastlefieldManchesterAirport("Navy Line", "Deansgate-Castlefield - Manchester Airport", "2120"),
    // Pink
    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line", "Rochdale - Manchester - East Didsbury", "845"),
    // Purple
    EtihadPiccadillyAltrincham("Purple Line", "Etihad Campus - Piccadilly - Altrincham", "2173"),
    // Red
    CornbrookTheTraffordCentre("Red Line", "Cornbrook - The Trafford Centre", "849"),
    // Yellow
    PiccadillyVictoria("Yellow Line", "Piccadilly - Victoria", "844");

    private final String shortName;
    private final String longName; // diagnostics only
    private final IdFor<Route> id;

    KnownTramRouteEnum(String shortName, String longName, String id) {
        this.longName = longName;
        this.shortName = shortName;
        this.id = Route.createId(id);
    }

    @Override
    public TransportMode mode() {
        return TransportMode.Tram;
    }

    /**
     * @return short name for a route
     */
    @Override
    public String shortName() {
        return shortName;
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
        return new MutableRoute(id, shortName, longName, TestEnv.MetAgency(), TransportMode.Tram);
    }
}
