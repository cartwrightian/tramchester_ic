package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.reference.TFGMRouteNames;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;
import static com.tramchester.testSupport.UpcomingDates.summer2026MajorClosure;

public class KnownTramRoute {

    public static final TramDate cutoverDate = TramDate.of(2026,7,6);

    // missing from tfgm data
    public static final String MISSING_ROUTE_ID_PREFIX = "xxx";

    public static @NotNull KnownTramRouteEnum getYellow(TramDate date) {
        return getFinder(date).singleRoute(Yellow);
    }

    public static @NotNull KnownTramRouteEnum getRed(TramDate date) {
        return getFinder(date).singleRoute(Red);
    }

    public static @NotNull KnownTramRouteEnum getPurple(TramDate date) {
        return getFinder(date).singleRoute(Purple);
    }

    public static @NotNull KnownTramRouteEnum getPink(TramDate date) {
        return getFinder(date).singleRoute(Pink);
    }

    public static @NotNull KnownTramRouteEnum getNavy(TramDate date) {
        return getFinder(date).singleRoute(Navy);
    }

    public static @NotNull KnownTramRouteEnum getGreen(TramDate date) {
        return getFinder(date).singleRoute(Green);
    }

    public static @NotNull KnownTramRouteEnum getBlue(TramDate date) {
        return getFinder(date).singleRoute(Blue);
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

    public static Set<KnownTramRouteEnum> getFor(final TramDate date) {

        final Set<KnownTramRouteEnum> routes = new HashSet<>();

        FindCurrentRouteFromLine find = new FindCurrentRouteFromLine(date);

        if (summer2026MajorClosure.contains(date)) {

            // not tracking buses

        } else {
            routes.add(find.singleRoute(Red));
            routes.add(find.singleRoute(Blue));
            routes.add(find.singleRoute(Purple));
            routes.add(find.singleRoute(Green));
        }

        routes.add(find.singleRoute(Yellow));
        routes.add(find.singleRoute(Navy));
        routes.add(find.singleRoute(Pink));

        return routes;
    }

    public static KnownTramRouteEnum findFor(final TFGMRouteNames line, final TramDate date) {
        return new FindCurrentRouteFromLine(date).singleRoute(line);
    }

    public static FindCurrentRouteFromLine getFinder(final TramDate date) {
        return new FindCurrentRouteFromLine(date);
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }

    public static class FindCurrentRouteFromLine {
        private final TramDate date;

        public FindCurrentRouteFromLine(TramDate date) {
            this.date = date;
        }

        /***
         * Will match routes with *same* validity date but only if they have different Route Ids, if Ids
         * clash then will throw
         * @param line The line to find routes
         * @return The date the routes must be valid for
         */
        public Set<KnownTramRouteEnum> multipleRoutes(final TFGMRouteNames line) {
            final SortedMap<TramDate, Set<KnownTramRouteEnum>> routesForDate = getKnownByDate(line);

            Map.Entry<TramDate, Set<KnownTramRouteEnum>> latest = routesForDate.lastEntry();

            Set<KnownTramRouteEnum> matched = latest.getValue();

            IdSet<Route> idCheck = matched.stream().map(KnownTramRouteEnum::getId).collect(IdSet.idCollector());

            if (idCheck.size()!=matched.size()) {
                throw new RuntimeException("Matched for " + line + " on date " + date + " not unique ids "
                 + matched + " ids " + idCheck);
            }

            return matched;
        }

        /***
         * Throws if finds more than one result for the given data i.e. same validity date
         * @param line The line to find a route for
         * @return The date the route must be valid for
         */
        public KnownTramRouteEnum singleRoute(final TFGMRouteNames line) {
            // date ordered, only if valid on or after the date
            final Map<TramDate, Set<KnownTramRouteEnum>> routesForDate = getKnownByDate(line);

            Map<TramDate, Set<KnownTramRouteEnum>> clashes = routesForDate.entrySet().stream().
                    filter(items -> items.getValue().size()>1).
                    collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!clashes.isEmpty()) {
                throw new RuntimeException("Too many routes for " + date + " " + line + "\n" + routesForDate);
            }

            // now should just have date -> single item
            List<KnownTramRouteEnum> valid = routesForDate.values().stream().
                    flatMap(Collection::stream).
                    sorted(Comparator.comparing(KnownTramRouteEnum::getValidFrom)).
                    toList();

            final KnownTramRouteEnum latest = valid.getLast();
            if (!latest.getId().isValid()) {
                throw new RuntimeException(latest + " has invalid id for date " + date);
            }
            return latest;
        }

        private @NonNull SortedMap<TramDate, Set<KnownTramRouteEnum>> getKnownByDate(final TFGMRouteNames line) {
            boolean dateIsSunday = (date.getDayOfWeek() == DayOfWeek.SUNDAY);
            final List<KnownTramRouteEnum> dateOrdered = Arrays.stream(KnownTramRouteEnum.values()).
                    filter(known -> known.line().equals(line)).
                    filter(known -> !known.sundayOnly() || (dateIsSunday && date.equals(known.getValidFrom()))).
                    filter(known -> date.isEqual(known.getValidFrom()) || date.isAfter(known.getValidFrom())).
                            toList();
            if (dateOrdered.isEmpty()) {
                throw new RuntimeException("No match for " + line.getShortName() + " on " + date);
            }

            // see if we have more than one candidate for the date
            Map<TramDate, Set<KnownTramRouteEnum>> routesForDate = dateOrdered.stream().collect(
                    Collectors.toMap(KnownTramRouteEnum::getValidFrom, Collections::singleton, SetUtils::union));

            SortedMap<TramDate, Set<KnownTramRouteEnum>> sortedByDate = new TreeMap<>(TramDate::compareTo);

            sortedByDate.putAll(routesForDate);

            return sortedByDate;
        }

        public KnownTramRouteEnum exactMatchWith(final Route expected) {
            TFGMRouteNames routeName = ((TramRouteId)expected.getId()).getRouteName();
            Set<KnownTramRouteEnum> matchLine = multipleRoutes(routeName);

            List<KnownTramRouteEnum> matchId = matchLine.stream().
                    filter(match -> match.getId().equals(expected.getId())).
                    toList();

            if (matchId.size()!=1) {
                throw new RuntimeException("Matched wrong number " + expected + " from " + matchLine);
            }

            return matchId.getFirst();
        }
    }

}
