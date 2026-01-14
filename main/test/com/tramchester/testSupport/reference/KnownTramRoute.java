package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TFGMRouteNames;
import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;

public class KnownTramRoute {

    public static final TramDate latestCutoverDate = TramDate.of(2026,1,11);

    // missing from tfgm data
    public static final String MISSING_ROUTE_ID = "";

    public static @NotNull KnownTramRouteEnum getYellow(TramDate date) {
        return getFinder(date).apply(Yellow);
    }

    public static @NotNull KnownTramRouteEnum getRed(TramDate date) {
        return getFinder(date).apply(Red);
    }

    public static @NotNull KnownTramRouteEnum getPurple(TramDate date) {
        return getFinder(date).apply(Purple);
    }

    public static @NotNull KnownTramRouteEnum getPink(TramDate date) {
        return getFinder(date).apply(Pink);
    }

    public static @NotNull KnownTramRouteEnum getNavy(TramDate date) {
        return getFinder(date).apply(Navy);
    }

    public static @NotNull KnownTramRouteEnum getGreen(TramDate date) {
        return getFinder(date).apply(Green);
    }

    public static @NotNull KnownTramRouteEnum getBlue(TramDate date) {
        return getFinder(date).apply(Blue);
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

        Function<TFGMRouteNames, KnownTramRouteEnum> find = getFinder(date);

        if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            if (date.isAfter(TramDate.of(2026, 1, 31))) {
                routes.add(find.apply(Green));
            }
        } else { // Not Sunday
            routes.add(find.apply(Green));
        }

        routes.add(find.apply(Yellow));
        routes.add(find.apply(Blue));
        routes.add(find.apply(Red));
        routes.add(find.apply(Navy));
        routes.add(find.apply(Pink));
        routes.add(find.apply(Purple));

        return routes;
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
            throw new RuntimeException(matched + " has invalid id for date " + date);
        }
        return matched;
    }

    private static Function<TFGMRouteNames, KnownTramRouteEnum> getFinder(final TramDate date) {
        return tfgmRouteNames -> findFor(tfgmRouteNames, date);
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }

}
