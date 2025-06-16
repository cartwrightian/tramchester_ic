package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.testSupport.UpcomingDates;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class KnownTramRoute {

    public static final TramDate latestCutoverDate = TramDate.of(2025,6,13);

    // missing from tfgm data
    public static final String MISSING_ROUTE_ID = "";

    /***
     * @return Yellow route
     */
    public static @NotNull KnownTramRouteEnum getYellow(TramDate date) {
        return findFor(TFGMRouteNames.Yellow, date);
    }

    /***
     * @return Red route
     */
    public static @NotNull KnownTramRouteEnum getRed(TramDate date) {
        return findFor(TFGMRouteNames.Red, date);
    }

    /***
     * @return Purple route
     */
    public static @NotNull KnownTramRouteEnum getPurple(TramDate date) {
        return findFor(TFGMRouteNames.Purple, date);
    }

    /***
     * @return Pink route
     */
    public static @NotNull KnownTramRouteEnum getPink(TramDate date) {
        return findFor(TFGMRouteNames.Pink, date);
    }

    /***
     * @return Navy route
     */
    public static @NotNull KnownTramRouteEnum getNavy(TramDate date) {
        return findFor(TFGMRouteNames.Navy, date);
    }

    /***
     * @return Green route
     */
    public static @NotNull KnownTramRouteEnum getGreen(TramDate date) {
        return findFor(TFGMRouteNames.Green, date);
    }

    /***
     * @return Blue route
     */
    public static @NotNull KnownTramRouteEnum getBlue(TramDate date) {
        return findFor(TFGMRouteNames.Blue, date);
    }

    public static KnownTramRouteEnum findFor(final TFGMRouteNames line, final TramDate date) {
        final List<KnownTramRouteEnum> find = Arrays.stream(KnownTramRouteEnum.values()).
                filter(knownTramRoute -> knownTramRoute.line().equals(line)).
                filter(knownTramRoute -> date.isEqual(knownTramRoute.getValidFrom()) || date.isAfter(knownTramRoute.getValidFrom())).
                sorted(Comparator.comparing(KnownTramRouteEnum::getValidFrom)).
                toList();
        if (find.isEmpty()) {
            throw new RuntimeException("No match for " + line.getShortName() + " on " + date);
        }
        final KnownTramRouteEnum matched = find.getLast(); // time ordered
        if (!matched.getId().isValid()) {
            throw new RuntimeException(matched + " is not valid for date " + date);
        }
        return matched;
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

        routes.add(getGreen(date));

        if (!UpcomingDates.PiccGardensWorksummer2025.contains(date)) {
            routes.add(getPurple(date));
            routes.add(getYellow(date));
        }

        routes.add(getBlue(date));
        routes.add(getRed(date));
        routes.add(getNavy(date));
        routes.add(getPink(date));

        return routes;
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }

}
