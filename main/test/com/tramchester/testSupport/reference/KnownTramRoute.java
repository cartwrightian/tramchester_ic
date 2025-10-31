package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TFGMRouteNames;
import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;
import static com.tramchester.testSupport.UpcomingDates.*;

public class KnownTramRoute {

    public static final TramDate latestCutoverDate = TramDate.of(2025,10,25);

    // missing from tfgm data
    public static final String MISSING_ROUTE_ID = "";

    /***
     * @return Replacement Buses
     */
    public static @NotNull KnownTramRouteEnum getBusOne(TramDate date) {
        return findFor(BusOne, date);
    }

    public static @NotNull KnownTramRouteEnum getBusTwo(TramDate date) {
        return findFor(BusTwo, date);
    }

    public static @NotNull KnownTramRouteEnum getBusThree(TramDate date) {
        return findFor(BusThree, date);
    }


    /***
     * @return Yellow route
     */
    public static @NotNull KnownTramRouteEnum getYellow(TramDate date) {
        return findFor(Yellow, date);
    }

    /***
     * @return Red route
     */
    public static @NotNull KnownTramRouteEnum getRed(TramDate date) {
        return findFor(Red, date);
    }

    /***
     * @return Purple route
     */
    public static @NotNull KnownTramRouteEnum getPurple(TramDate date) {
        return findFor(Purple, date);
    }

    /***
     * @return Pink route
     */
    public static @NotNull KnownTramRouteEnum getPink(TramDate date) {
        return findFor(Pink, date);
    }

    /***
     * @return Navy route
     */
    public static @NotNull KnownTramRouteEnum getNavy(TramDate date) {
        return findFor(Navy, date);
    }

    /***
     * @return Green route
     */
    public static @NotNull KnownTramRouteEnum getGreen(TramDate date) {
        return findFor(Green, date);
    }

    /***
     * @return Blue route
     */
    public static @NotNull KnownTramRouteEnum getBlue(TramDate date) {
        return findFor(Blue, date);
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

        if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            if (VictoriaAndRochdaleLineWorks.equals(date) || VictoriaNov2025.equals(date)) {
                routes.add(getYellow(date));
            }

            if (TraffordBar2025.contains(date)) {
                routes.add(getYellow(date));
                routes.add(getBusOne(date));
            }

            if (date.equals(TramDate.of(2025, 11, 9)) || date.equals(TramDate.of(2025, 11, 30))) {
                routes.add(getYellow(date));
            }
        } else {
            if (!TraffordBar2025.contains(date)) {
                routes.add(getGreen(date));
            }
            routes.add(getYellow(date));
        }

//        if (VictoriaAndRochdaleLineWorks.equals(date)) {
//            routes.add(getBusOne(date));
//        }

        if (TraffordBar2025.contains(date)) {
            routes.add(getBusOne(date));
            routes.add(getBusTwo(date));
            routes.add(getBusThree(date));
        }

        routes.add(getBlue(date));
        routes.add(getRed(date));
        routes.add(getNavy(date));
        routes.add(getPink(date));
        routes.add(getPurple(date));

        return routes;
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }

}
