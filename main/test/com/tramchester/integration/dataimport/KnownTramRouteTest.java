package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.KnownTramRouteEnum;
import com.tramchester.testSupport.reference.TestRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.KnownTramRoute.getYellow;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
class KnownTramRouteTest {
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepository;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void setUp() {
        routeRepository = componentContainer.get(RouteRepository.class);
        when = TestEnv.testDay();
    }

    private Stream<TramDate> getDateRange() {
        return UpcomingDates.getUpcomingDates();
    }

    @Test
    void shouldHaveSomeDatesToTest() {
        assertFalse(getDateRange().collect(Collectors.toSet()).isEmpty());
    }

    @Test
    void shouldHaveExpectedRouteIdForBlue() {
        checkRouteIdFor(KnownTramRoute::getBlue, false);
    }

    @Test
    void shouldHaveExpectedRouteIdForNavy() {
        checkRouteIdFor(KnownTramRoute::getNavy, false);
    }

    @Test
    void shouldHaveExpectedRouteIdForGreen() {
        checkRouteIdFor(KnownTramRoute::getGreen, true);
    }

    @Test
    void shouldHaveExpectedRouteIdForPink() {
        checkRouteIdFor(KnownTramRoute::getPink, false);
    }

    @Test
    void shouldHaveExpectedRouteIdForPurple() {
        checkRouteIdFor(KnownTramRoute::getPurple, false);
    }

    @Test
    void shouldHaveExpectedRouteIdForRed() {
        checkRouteIdFor(KnownTramRoute::getRed, false);
    }

    @Test
    void shouldHaveExpectedRouteIdForYellow() {
        checkRouteIdFor(KnownTramRoute::getYellow, true);
    }

    void checkRouteIdFor(Function<TramDate, KnownTramRouteEnum> function, boolean skipSunday) {

        List<TramDate> missingFromDataOnDates = new ArrayList<>();
        getDateRange().
                filter(date -> !(skipSunday && date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) ).
                sorted(TramDate::compareTo).
                forEach(date -> {
                    final IdSet<Route> loadedIds = getLoadedTramRoutes(date).collect(IdSet.collector());
                    final KnownTramRouteEnum testRoute = function.apply(date);
                    if (checkRouteOnDate(date, testRoute) && !loadedIds.contains(testRoute.getId())) {
                        missingFromDataOnDates.add(date);
                    }
            });

        List<String> diag = missingFromDataOnDates.stream().map(date -> "On date " + date + " test route " + function.apply(date) + " with id " +
                function.apply(date).getId() + " is missing from data: " + shortNameMatch(function, date) + System.lineSeparator()).toList();
        assertTrue(missingFromDataOnDates.isEmpty(), diag.toString());
    }

    private boolean checkRouteOnDate(TramDate date, KnownTramRouteEnum testRoute) {
//        if (testRoute.line()== TFGMRouteNames.Green) {
//            return !LateMayBankHol2025.equals(date);
//        }
        return true;
    }

    private String shortNameMatch(Function<TramDate, KnownTramRouteEnum> function, TramDate date) {
        String shortName = function.apply(date).shortName();
        IdSet<Route> matchedShortNames = getLoadedTramRoutes(date).
                filter(route -> route.getShortName().equals(shortName)).
                map(Route::getId).
                collect(IdSet.idCollector());
        return matchedShortNames.toString();
    }

    @Test
    void shouldHaveCorrectIds() {

        getDateRange().forEach(date -> {
            IdSet<Route> loadedIds = getLoadedTramRoutes(date).collect(IdSet.collector());

            IdSet<Route> knownTramOnDates = KnownTramRoute.getFor(date).stream().map(TestRoute::getId).
                    collect(IdSet.idCollector());

            IdSet<Route> mismatch = IdSet.disjunction(loadedIds, knownTramOnDates);

            assertTrue(mismatch.isEmpty(), "on " + date + " MISMATCH \n" + mismatch + "\n between LOADED \n" + loadedIds + " AND \n" + knownTramOnDates);
        });
    }

    @Test
    void shouldHaveExpectedNumberOfTramRoutes() {
        final Set<Route> loaded = routeRepository.getRoutesRunningOn(when, TramsOnly);

        assertEquals(loaded.size(), KnownTramRoute.getFor(when).size());
    }

    @Test
    void shouldHaveShortNameMatching() {

        getDateRange().forEach(date -> {
            final Set<Route> loadedRoutes = getLoadedTramRoutes(date).collect(Collectors.toSet());
            KnownTramRoute.getFor(date).forEach(knownTramRoute -> {
                String prefix = "On " + date + " ";
                List<Route> findLoadedFor = loadedRoutes.stream().
                        filter(loadedRoute -> loadedRoute.getShortName().equals(knownTramRoute.shortName())).toList();
                assertEquals(1, findLoadedFor.size(), prefix + "could not find loaded route using short name match for " + knownTramRoute);
            });
        });
    }

    @Test
    void shouldNotHaveUnknownTramRoutes() {
        TramDate start = TramDate.from(TestEnv.LocalNow());

        DateRange dateRange = DateRange.of(start, when.plusWeeks(4));

        SortedMap<TramDate, IdSet<Route>> unexpectedLoadedForDate = new TreeMap<>();

        dateRange.stream().forEach(date -> {
            final IdSet<Route> known = KnownTramRoute.getFor(date).stream().
                    map(TestRoute::getId).
                    collect(IdSet.idCollector());
            final Set<Route> loadedRoutes = getLoadedTramRoutes(date).
                    filter(route -> !known.contains(route.getId())).
                    collect(Collectors.toSet());

            if (!loadedRoutes.isEmpty()) {
                unexpectedLoadedForDate.put(date, loadedRoutes.stream().collect(IdSet.collector()));
            }
        });

        assertTrue(unexpectedLoadedForDate.isEmpty(), "Mismatch on known routes, unexpected loaded routes were: "
                + unexpectedLoadedForDate);
    }

    @Test
    void shouldNotHaveUnusedKnownTramRoutesForDate() {
        TramDate start = TramDate.from(TestEnv.LocalNow());

        DateRange dateRange = DateRange.of(start, when.plusWeeks(6));

        SortedMap<TramDate, Set<TestRoute>> unusedForDate = new TreeMap<>();

        dateRange.stream().
                filter(date -> !(UpcomingDates.isChristmasDay(date) || UpcomingDates.isBoxingDay(date))).
                forEach(date -> {
                    final IdSet<Route> loaded = getLoadedTramRoutes(date).collect(IdSet.collector());

                    final Set<TestRoute> knownButUnused = KnownTramRoute.getFor(date).stream().
                            filter(knownTramRoute -> !loaded.contains(knownTramRoute.getId())).
                            collect(Collectors.toSet());
                    if (!knownButUnused.isEmpty()) {
                        unusedForDate.put(date, knownButUnused);
                    }
                });

        assertTrue(unusedForDate.isEmpty(), "For dates " + unusedForDate.keySet() +
                "\n Have known but not loaded routes " + unusedForDate);
    }

    @Test
    void shouldHaveCorrectDateForKnownRoutes() {
        EnumSet<KnownTramRouteEnum> knowRoutes = KnownTramRouteEnum.validRoutes();

        assertFalse(knowRoutes.isEmpty());

        knowRoutes.forEach(known -> {
            assertTrue(routeRepository.hasRouteId(known.getId()), known + "(" +known.getId() + ") is missing from repo");
            Route actual = routeRepository.getRouteById(known.getId());
            assertTrue(actual.getDateRange().contains(known.getValidFrom()), known.getValidFrom() + " for " +
                    known + " not within " + actual.getDateRange());
        });
    }

    @Test
    void shouldCheckForMissingRouteSpring2025Closures() {
        // TODO needed for each route?

        TestRoute piccadillyVictoria = getYellow(when);
        String shortName = piccadillyVictoria.shortName();

        Set<Route> matching = routeRepository.getRoutes().stream().
                filter(route -> route.getShortName().equals(shortName)).
                collect(Collectors.toSet());

        assertFalse(matching.isEmpty());

        Set<Route> foundForDate = matching.stream().filter(route -> route.isAvailableOn(when)).collect(Collectors.toSet());

        assertFalse(foundForDate.isEmpty(), "Not matching date " + when + " for " + HasId.asIds(matching));
    }

    @Test
    void shouldCheckForActualDatesYellowRouteIsAvailableFor() {
        TestRoute piccadillyVictoria = getYellow(when);
        String shortName = piccadillyVictoria.shortName();

        List<Route> matching = routeRepository.getRoutes().stream().
                filter(route -> route.getShortName().equals(shortName)).
                toList();

        assertEquals(1, matching.size(), HasId.asIds(matching));

        List<DateRange> ranges = matching.stream().map(Route::getDateRange).toList();

        boolean available = ranges.stream().anyMatch(range -> range.contains(when));

        assertTrue(available, when + " is missing from loaded " + ranges);

    }

    @NotNull
    private Stream<Route> getLoadedTramRoutes(final TramDate date) {
        return routeRepository.getRoutesRunningOn(date, EnumSet.of(Tram)).stream();
    }

}
