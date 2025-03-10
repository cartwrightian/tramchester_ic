package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.UpcomingDates.YorkStreetWorks2025;

public class KnownTramRoute {

    public static final TramDate revertDate = TramDate.of(2025, 2, 25);

    public static final TramDate marchCutover = TramDate.of(2025,3,13);

    private static final DateRange YorkStreetWorks2025MissingRoute = DateRange.of(YorkStreetWorks2025.getEndDate(),
            YorkStreetWorks2025.getEndDate().plusDays(5));

    /***
     * @return Replacement Bus Media City to Eccles
     */
//    public static @NotNull TestRoute getBusEcclesToMediaCity(TramDate date) {
//        return findFor("Replacement Bus 1", date);
//    }

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

        routes.add(getBuryManchesterAltrincham(date));

        if (!(YorkStreetWorks2025.contains(date) || YorkStreetWorks2025MissingRoute.contains(date))) {
            routes.add(getEtihadPiccadillyAltrincham(date));
        }

        routes.add(getPiccadillyVictoria(date));

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
