package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.testSupport.UpcomingDates;
import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KnownTramRoute {
    // Replacement buses
    public static final TestRoute BusEcclesToMediaCity = getBusEcclesToMediaCity();
    // Blue
    public static final TestRoute EcclesAshton = getEcclesAshton();
    // Green
    public static final TestRoute BuryManchesterAltrincham = getBuryManchesterAltrincham();
    // Navy
    public static final TestRoute DeansgateCastlefieldManchesterAirport = getDeansgateManchesterAirport();
    // Pink
    public static final TestRoute RochdaleShawandCromptonManchesterEastDidisbury = getShawandCromptonManchesterEastDidisbury();
    // Purple
    public static final TestRoute EtihadPiccadillyAltrincham = getEtihadPiccadillyAltrincham();
    // Red
    public static final TestRoute CornbrookTheTraffordCentre = getCornbrookTheTraffordCentre();
    // Yellow
    public static final TestRoute PiccadillyVictoria = getPiccadillyVictoria();

    private static @NotNull KnownTramRouteEnum getPiccadillyVictoria() {
        return KnownTramRouteEnum.PiccadillyVictoria;
    }

    private static @NotNull KnownTramRouteEnum getBusEcclesToMediaCity() {
        return KnownTramRouteEnum.BusEcclesToMediaCity;
    }

    private static @NotNull KnownTramRouteEnum getCornbrookTheTraffordCentre() {
        return KnownTramRouteEnum.CornbrookTheTraffordCentre;
    }

    private static @NotNull KnownTramRouteEnum getEtihadPiccadillyAltrincham() {
        return KnownTramRouteEnum.EtihadPiccadillyAltrincham;
    }

    private static @NotNull KnownTramRouteEnum getShawandCromptonManchesterEastDidisbury() {
        return KnownTramRouteEnum.RochdaleShawandCromptonManchesterEastDidisbury;
    }

    private static @NotNull KnownTramRouteEnum getDeansgateManchesterAirport() {
        return KnownTramRouteEnum.DeansgateCastlefieldManchesterAirport;
    }

    private static @NotNull KnownTramRouteEnum getBuryManchesterAltrincham() {
        return KnownTramRouteEnum.BuryManchesterAltrincham;
    }

    private static @NotNull KnownTramRouteEnum getEcclesAshton() {
        return KnownTramRouteEnum.EcclesAshton;
    }

    public static Set<TestRoute> find(final Set<Route> routes) {
        return routes.stream().
                map(Route::getId).
                map(KnownTramRoute::fromId).
                collect(Collectors.toSet());
    }

    private static TestRoute fromId(final IdFor<Route> routeId) {
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
        Set<TestRoute> routes = new HashSet<>();

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
