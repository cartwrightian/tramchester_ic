package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.testSupport.UpcomingDates;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KnownTramRoute {
    // Replacement buses
    public static final KnownTramRouteEnum BusEcclesToMediaCity = KnownTramRouteEnum.BusEcclesToMediaCity;
    // Blue
    public static final KnownTramRouteEnum EcclesAshton = KnownTramRouteEnum.EcclesAshton;
    // Green
    public static final KnownTramRouteEnum BuryManchesterAltrincham = KnownTramRouteEnum.BuryManchesterAltrincham;
    // Navy
    public static final KnownTramRouteEnum DeansgateCastlefieldManchesterAirport = KnownTramRouteEnum.DeansgateCastlefieldManchesterAirport;
    // Pink
    public static final KnownTramRouteEnum RochdaleShawandCromptonManchesterEastDidisbury = KnownTramRouteEnum.RochdaleShawandCromptonManchesterEastDidisbury;
    // Purple
    public static final KnownTramRouteEnum EtihadPiccadillyAltrincham = KnownTramRouteEnum.EtihadPiccadillyAltrincham;
    // Red
    public static final KnownTramRouteEnum CornbrookTheTraffordCentre = KnownTramRouteEnum.CornbrookTheTraffordCentre;
    // Yellow
    public static final KnownTramRouteEnum PiccadillyVictoria = KnownTramRouteEnum.PiccadillyVictoria;

    public static Set<TestRoute> find(final Set<Route> routes) {
        return routes.stream().
                map(Route::getId).
                map(KnownTramRoute::fromId).
                collect(Collectors.toSet());
    }

    static TestRoute fromId(final IdFor<Route> routeId) {
        Optional<KnownTramRouteEnum> find = Arrays.stream(KnownTramRouteEnum.values()).
                filter(knownTramRoute -> knownTramRoute.getId().equals(routeId)).findFirst();
        if (find.isPresent()) {
            return find.get();
        } else {
            throw new RuntimeException("Could not find a known tram route with ID " + routeId);
        }
    }

    public static int numberOn(final TramDate date) {
        return getFor(date).size();
    }

    public static Set<TestRoute> getFor(final TramDate date) {
        Set<TestRoute> routes = new HashSet<>(); // EnumSet.noneOf(KnownTramRoute.class);

        final TramDate startDate = UpcomingDates.MediaCityEcclesWorks2025.getStartDate().minusDays(1);

        if (startDate.isBefore(date) &&
                date.isBefore(TramDate.of(2025,3, 28))) {
            routes.add(BusEcclesToMediaCity);
        }

        if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            routes.add(BuryManchesterAltrincham);

        } else {
            routes.add(BuryManchesterAltrincham);
        }

        routes.add(PiccadillyVictoria);
        routes.add(EtihadPiccadillyAltrincham);

        routes.add(EcclesAshton);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(DeansgateCastlefieldManchesterAirport);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);

        return routes;
    }

    public static KnownTramRouteEnum[] values() {
        return KnownTramRouteEnum.values();
    }
}
