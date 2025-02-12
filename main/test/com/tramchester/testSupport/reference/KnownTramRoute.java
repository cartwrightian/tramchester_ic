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

    /***
     * @return Yellow route
     */
    public static @NotNull TestRoute getPiccadillyVictoria() {
        return KnownTramRouteEnum.PiccadillyVictoria;
    }

    /***
     * @return Replacement Bus Media City to Eccles
     */
    public static @NotNull TestRoute getBusEcclesToMediaCity() {
        return KnownTramRouteEnum.BusEcclesToMediaCity;
    }

    /***
     * @return Red route
     */
    public static @NotNull TestRoute getCornbrookTheTraffordCentre() {
        return KnownTramRouteEnum.CornbrookTheTraffordCentre;
    }

    /***
     * @return Purple route
     */
    public static @NotNull TestRoute getEtihadPiccadillyAltrincham() {
        return KnownTramRouteEnum.EtihadPiccadillyAltrincham;
    }

    /***
     * @return Pink route
     */
    public static @NotNull TestRoute getShawandCromptonManchesterEastDidisbury() {
        return KnownTramRouteEnum.RochdaleShawandCromptonManchesterEastDidisbury;
    }

    /***
     * @return Navy route
     */
    public static @NotNull TestRoute getDeansgateManchesterAirport() {
        return KnownTramRouteEnum.DeansgateCastlefieldManchesterAirport;
    }

    /***
     * @return Green route
     */
    public static @NotNull TestRoute getBuryManchesterAltrincham() {
        return KnownTramRouteEnum.BuryManchesterAltrincham;
    }

    /***
     * @return Blue route
     */
    public static @NotNull TestRoute getEcclesAshton() {
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
            routes.add(getBusEcclesToMediaCity());
        }

        if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            routes.add(getBuryManchesterAltrincham());

        } else {
            routes.add(getBuryManchesterAltrincham());
        }

        routes.add(getPiccadillyVictoria());
        routes.add(getEtihadPiccadillyAltrincham());

        routes.add(getEcclesAshton());
        routes.add(getCornbrookTheTraffordCentre());
        routes.add(getDeansgateManchesterAirport());
        routes.add(getShawandCromptonManchesterEastDidisbury());

        return routes;
    }

    public static KnownTramRouteEnum[] values() {
        return KnownTramRouteEnum.values();
    }
}
