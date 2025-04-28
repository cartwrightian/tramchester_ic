package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateRanges;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TFGMRouteNames;
import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

public class KnownTramRoute {

    public static final TramDate endAprilCutover = TramDate.of(2025, 4, 24);
    public static final TramDate startMayCutover = TramDate.of(2025,5,3);

    private static final DateRanges replacementBusOneDates = new DateRanges(
            DateRange.of(TramDate.of(2025,5,3), 2),
            DateRange.of(TramDate.of(2025, 5, 10), 1),
            DateRange.of(TramDate.of(2025, 5, 17), 1),
            DateRange.of(TramDate.of(2025, 5, 24), 2));

    // missing from tfgm data
    public static final String MISSING_ROUTE = "";

    /***
     * @return Yellow route
     */
    public static @NotNull TestRoute getReplacementBusOne(TramDate date) {
        return findFor(TFGMRouteNames.BusOne, date);
    }

    /***
     * @return Yellow route
     */
    public static @NotNull TestRoute getYellow(TramDate date) {
        return findFor(TFGMRouteNames.Yellow, date);
    }

    /***
     * @return Red route
     */
    public static @NotNull TestRoute getRed(TramDate date) {
        return findFor(TFGMRouteNames.Red, date);
    }

    /***
     * @return Purple route
     */
    public static @NotNull TestRoute getPurple(TramDate date) {
        return findFor(TFGMRouteNames.Purple, date);
    }

    /***
     * @return Pink route
     */
    public static @NotNull TestRoute getPink(TramDate date) {
        return findFor(TFGMRouteNames.Pink, date);
    }

    /***
     * @return Navy route
     */
    public static @NotNull TestRoute getNavy(TramDate date) {
        return findFor(TFGMRouteNames.Navy, date);
    }

    /***
     * @return Green route
     */
    public static @NotNull TestRoute getGreen(TramDate date) {
        return findFor(TFGMRouteNames.Green, date);
    }

    /***
     * @return Blue route
     */
    public static @NotNull TestRoute getBlue(TramDate date) {
        return findFor(TFGMRouteNames.Blue, date);
    }

    public static TestRoute findFor(final TFGMRouteNames line, final TramDate date) {
        List<KnownTramRouteEnum> find = Arrays.stream(KnownTramRouteEnum.values()).
                filter(knownTramRoute -> knownTramRoute.line().equals(line)).
                filter(knownTramRoute -> date.isEqual(knownTramRoute.getValidFrom()) || date.isAfter(knownTramRoute.getValidFrom())).
                sorted(Comparator.comparing(KnownTramRouteEnum::getValidFrom)).
                toList();
        if (find.isEmpty()) {
            throw new RuntimeException("No match for " + line.getShortName() + " on " + date);
        }
        final KnownTramRouteEnum matched = find.getLast();
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

        if (date.isAfter(endAprilCutover) || date.equals(endAprilCutover)) {
//            if (date.getDayOfWeek()!= DayOfWeek.SUNDAY) {
//                routes.add(getYellow(date));
//            }
            routes.add(getPurple(date));
        }

        if (date.isAfter(startMayCutover) || date.equals(startMayCutover)) {
            routes.add(getYellow(date));
        } else if (date.getDayOfWeek()!= DayOfWeek.SUNDAY) {
            routes.add(getYellow(date));
        }

        routes.add(getGreen(date));
        routes.add(getBlue(date));
        routes.add(getRed(date));
        routes.add(getNavy(date));
        routes.add(getPink(date));

        if (replacementBusOneDates.contains(date)) {
            routes.add(getReplacementBusOne(date));
        }

        return routes;
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }

}
