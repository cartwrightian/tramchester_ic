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
import static com.tramchester.testSupport.UpcomingDates.summerBankHol2026;
import static com.tramchester.testSupport.UpcomingDates.sundaySept202ClosureNotPublished;

public class KnownTramRoute {

    public static final TramDate cutoverDate = TramDate.of(2026,7,29);

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

        // date.equals(sundaySept2026Closure) ||
        if (! (date.equals(sundaySept202ClosureNotPublished) || date.equals(summerBankHol2026)) ) {
            routes.add(find.singleRoute(Red));
            routes.add(find.singleRoute(Blue));
            routes.add(find.singleRoute(Purple));
            routes.add(find.singleRoute(Yellow));
            routes.add(find.singleRoute(Navy));
            routes.add(find.singleRoute(Pink));
        }

        if (date.getDayOfWeek()==DayOfWeek.SUNDAY) {
            if (date.isBefore(TramDate.of(2026, 8, 9))) {
                routes.add(find.singleRoute(Green));
            }
        } else {
            if (!date.equals(summerBankHol2026)) {
                routes.add(find.singleRoute(Green));
            }
        }

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

            // remove known routes not applicable for current date
            final List<KnownTramRouteEnum> possibleForDate = Arrays.stream(KnownTramRouteEnum.values()).
                    filter(known -> known.line().equals(line)).
                    filter(known -> !date.isBefore(known.getValidFrom())).
                    sorted(Comparator.comparing(KnownTramRouteEnum::getValidFrom)).
                    toList();

            DayOfWeek dayOfWeek = date.getDayOfWeek();

            final List<KnownTramRouteEnum> modifiersApplied;
            if (dayOfWeek==DayOfWeek.SATURDAY || dayOfWeek==DayOfWeek.SUNDAY) {
                final EnumSet<KnownTramRouteEnum.DaysMod> applicableMods = applicableMods(dayOfWeek);
                final List<KnownTramRouteEnum> possibleForDateWithMods = possibleForDate.stream().
                        filter(known -> applicableMods.contains(known.modifiers())).
                        filter(known -> dateMatchRequired(known, date)).
                        toList();
                if (possibleForDateWithMods.isEmpty()) {
                    modifiersApplied = possibleForDate;
                } else {
                    modifiersApplied = possibleForDateWithMods;
                }
            } else {
                modifiersApplied = possibleForDate.stream().
                        filter(known -> known.modifiers()== KnownTramRouteEnum.DaysMod.none).
                        toList();
            }

            final Map<TramDate, Set<KnownTramRouteEnum>> routesForDate = modifiersApplied.stream().collect(
                    Collectors.toMap(KnownTramRouteEnum::getValidFrom, Collections::singleton, SetUtils::union));

            final SortedMap<TramDate, Set<KnownTramRouteEnum>> sortedByDate = new TreeMap<>(TramDate::compareTo);
            sortedByDate.putAll(routesForDate);
            return sortedByDate;
        }

        private boolean dateMatchRequired(KnownTramRouteEnum known, TramDate date) {
            KnownTramRouteEnum.DaysMod mods = known.modifiers();
            if (mods==KnownTramRouteEnum.DaysMod.saturdayOnly || mods ==KnownTramRouteEnum.DaysMod.sundayOnly) {
                // specific to one date - TODO change to TodayOny?
                return date.equals(known.getValidFrom());
            }
            return true;
        }

        private static @NonNull EnumSet<KnownTramRouteEnum.DaysMod> applicableMods(DayOfWeek dayOfWeek) {
            final EnumSet<KnownTramRouteEnum.DaysMod> applicableMods = EnumSet.noneOf(KnownTramRouteEnum.DaysMod.class);
            if (dayOfWeek ==DayOfWeek.SATURDAY) {
                applicableMods.add(KnownTramRouteEnum.DaysMod.everySaturday);
                applicableMods.add(KnownTramRouteEnum.DaysMod.saturdayOnly);
            }
            if (dayOfWeek ==DayOfWeek.SUNDAY) {
                applicableMods.add(KnownTramRouteEnum.DaysMod.everySunday);
                applicableMods.add(KnownTramRouteEnum.DaysMod.sundayOnly);
            }
            return applicableMods;
        }

        private @NonNull SortedMap<TramDate, Set<KnownTramRouteEnum>> getKnownByDateOLD(final TFGMRouteNames line) {

            final List<KnownTramRouteEnum> dateOrdered = Arrays.stream(KnownTramRouteEnum.values()).
                    filter(known -> known.line().equals(line)).
                    filter(known -> checkModifiers(date, known)).
                    filter(known -> date.isEqual(known.getValidFrom()) || date.isAfter(known.getValidFrom())).
                            toList();
            if (dateOrdered.isEmpty()) {
                throw new RuntimeException("No match for " + line.getShortName() + " on " + date);
            }

            // see if we have more than one candidate for the date
            final Map<TramDate, Set<KnownTramRouteEnum>> routesForDate = dateOrdered.stream().collect(
                    Collectors.toMap(KnownTramRouteEnum::getValidFrom, Collections::singleton, SetUtils::union));

            final SortedMap<TramDate, Set<KnownTramRouteEnum>> sortedByDate = new TreeMap<>(TramDate::compareTo);

            sortedByDate.putAll(routesForDate);

            return sortedByDate;
        }

        private boolean checkModifiers(final TramDate date, final KnownTramRouteEnum known) {
            final boolean isSaturday = (date.getDayOfWeek() == DayOfWeek.SATURDAY);
            final boolean isSunday = (date.getDayOfWeek() == DayOfWeek.SUNDAY);
            final boolean matches = date.equals(known.getValidFrom());
            final boolean after = date.isAfter(known.getValidFrom());

            return switch (known.modifiers()) {
                case none -> matches || after;
                case sundayOnly -> isSunday && matches;
                case saturdayOnly -> isSaturday && matches;
                case everySunday -> isSunday && (matches || after);
                case everySaturday -> isSaturday && (matches || after);
            };

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
