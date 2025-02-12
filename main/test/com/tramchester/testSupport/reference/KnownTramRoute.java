package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.testSupport.UpcomingDates;
import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

public class KnownTramRoute {

    public static TramDate febCutOver = TramDate.of(2025,2,16);
    public static TramDate initialDate = TramDate.of(2014, 6, 1);

    /***
     * @return Replacement Bus Media City to Eccles
     */
    public static @NotNull TestRoute getBusEcclesToMediaCity(TramDate date) {
        return findFor("Replacement Bus 1", date);
    }

    /***
     * @return Replacement Bus Victoria to Piccadilly
     */
    public static @NotNull TestRoute getBusVictoriaPiccadilly(TramDate date) {
        return findFor("Replacement Bus 2", date);
    }

    /***
     * @return Yellow route
     */
    public static @NotNull TestRoute getPiccadillyVictoria(TramDate date) {
        return findFor("Yellow Line", date);
    }

    /***
     * @return Red route
     */
    public static @NotNull TestRoute getCornbrookTheTraffordCentre(TramDate date) {
        return findFor("Red Line", date);
    }

    /***
     * @return Purple route
     */
    public static @NotNull TestRoute getEtihadPiccadillyAltrincham(TramDate date) {
        return findFor("Purple Line", date);
    }

    /***
     * @return Pink route
     */
    public static @NotNull TestRoute getShawandCromptonManchesterEastDidisbury(TramDate date) {
        return findFor("Pink Line", date);
    }

    /***
     * @return Navy route
     */
    public static @NotNull TestRoute getDeansgateManchesterAirport(TramDate date) {
        return findFor("Navy Line", date);
    }

    /***
     * @return Green route
     */
    public static @NotNull TestRoute getBuryManchesterAltrincham(TramDate date) {
        return findFor("Green Line", date);
    }

    /***
     * @return Blue route
     */
    public static @NotNull TestRoute getEcclesAshton(TramDate date) {
        return findFor("Blue Line", date);
    }

    public static TestRoute findFor(final String shortName, final TramDate date) {
        List<KnownTramRouteEnum> matched = Arrays.stream(KnownTramRouteEnum.values()).
                filter(knownTramRoute -> knownTramRoute.shortName().equals(shortName)).
                filter(knownTramRoute -> date.isEqual(knownTramRoute.getValidFrom()) || date.isAfter(knownTramRoute.getValidFrom())).
                sorted(Comparator.comparing(KnownTramRouteEnum::getValidFrom)).
                toList();
        if (matched.isEmpty()) {
            throw new RuntimeException("No match for " + shortName + " on " + date);
        }
        return matched.getLast();
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
        final Set<TestRoute> routes = new HashSet<>();

        final TramDate startDate = UpcomingDates.MediaCityEcclesWorks2025.getStartDate().minusDays(1);

        if (startDate.isBefore(date) &&
                date.isBefore(TramDate.of(2025,3, 28))) {
            routes.add(getBusEcclesToMediaCity(date));
        }
        if (date.equals(TramDate.of(2025,2,23))) {
            routes.add(getBusVictoriaPiccadilly(date));
        }

        if (!date.getDayOfWeek().equals(DayOfWeek.SUNDAY) || !date.isAfter(TramDate.of(2025, 2, 15))) {
            routes.add(getBuryManchesterAltrincham(date));
        }

        routes.add(getPiccadillyVictoria(date));
        routes.add(getEtihadPiccadillyAltrincham(date));

        routes.add(getEcclesAshton(date));
        routes.add(getCornbrookTheTraffordCentre(date));
        routes.add(getDeansgateManchesterAirport(date));
        routes.add(getShawandCromptonManchesterEastDidisbury(date));

        return routes;
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }
}
