package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.SUNDAY;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRoute {

    // Blue
    EcclesAshton("Blue Line", "Eccles - Manchester - Ashton Under Lyne", "2119"),
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
    private final String longName;
    private final IdFor<Route> id;

    public static Set<KnownTramRoute> getFor(final TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.noneOf(KnownTramRoute.class);

        if (date.isAfter(TramDate.of(2025,1,1))) {
            if (!date.getDayOfWeek().equals(SUNDAY)) {
                routes.add(BuryManchesterAltrincham);
            }
        } else {
            routes.add(BuryManchesterAltrincham);
        }

        routes.add(PiccadillyVictoria);
        routes.add(EcclesAshton);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(DeansgateCastlefieldManchesterAirport);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);
        routes.add(EtihadPiccadillyAltrincham);

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

    public static Set<KnownTramRoute> find(final Set<Route> routes) {
        return routes.stream().
                map(Route::getId).
                map(KnownTramRoute::fromId).
                collect(Collectors.toSet());
    }

    private static KnownTramRoute fromId(final IdFor<Route> routeId) {
        Optional<KnownTramRoute> find = Arrays.stream(KnownTramRoute.values()).
                filter(knownTramRoute -> knownTramRoute.getId().equals(routeId)).findFirst();
        if (find.isPresent()) {
            return find.get();
        } else {
            throw new RuntimeException("Could not find a known tram route with ID " + routeId);
        }
    }

    public TransportMode mode() { return TransportMode.Tram; }

    /**
     * @return short name for a route
     */
    public String shortName() {
        return shortName;
    }

    /**
     * Should not use, seems to change frequency, prefer ID
     * @return long name, matching tfgm
     */
    @Deprecated
    public String longName() {
        return longName;
    }

    public IdFor<Route> getId() {
        return id;
    }

    public IdForDTO dtoId() {
        return IdForDTO.createFor(id);
    }

    public Route fake() {
        return new MutableRoute(id, shortName, longName, TestEnv.MetAgency(), TransportMode.Tram);
    }
}
