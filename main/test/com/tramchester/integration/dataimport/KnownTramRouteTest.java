package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
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
        return TestEnv.getUpcomingDates();
    }

    @Test
    void shouldHaveSomeDatesToTest() {
        assertFalse(getDateRange().collect(Collectors.toSet()).isEmpty());
    }

    @Test
    void shouldHaveCorrectIds() {

        getDateRange().forEach(date -> {
            IdSet<Route> loadedIds = getLoadedTramRoutes(date).collect(IdSet.collector());

            IdSet<Route> knownTramOnDates = KnownTramRoute.getFor(date).stream().map(KnownTramRoute::getId).
                    collect(IdSet.idCollector());

            IdSet<Route> mismatch = IdSet.disjunction(loadedIds, knownTramOnDates);

            assertTrue(mismatch.isEmpty(), "on " + date + " MISMATCH \n" + mismatch + "\n between LOADED \n" + loadedIds + " AND \n" + knownTramOnDates);
        });
    }

    @Test
    void shouldHaveExpectedNumberOfTramRoutes() {
        final Set<Route> loaded = routeRepository.getRoutesRunningOn(when);

        assertEquals(loaded.size(), KnownTramRoute.getFor(when).size());
    }

    /// Note: START HERE when diagnosing
    //  if route count correct then check dates, route might not be available on given dates
    @Disabled("Seem to be changing names frequently in source data")
    @Test
    void shouldHaveCorrectLongNamesForKnownRoutesForDates() {

        getDateRange().forEach(date -> {
            Set<String> loadedLongNames = getLoadedTramRoutes(date).map(Route::getName).collect(Collectors.toSet());

            Set<String> knownTramOnDates = KnownTramRoute.getFor(date).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

            Set<String> mismatch = SetUtils.disjunction(loadedLongNames, knownTramOnDates);

            assertTrue(mismatch.isEmpty(), "on " + date + " MISMATCH \n" + mismatch + "\n between LOADED \n" + loadedLongNames + " AND \n" + knownTramOnDates);
        });

    }

    @Test
    void shouldHaveShortNameMatching() {
        // Assumes long name match, if this fails get shouldHaveCorrectLongNamesForKnownRoutesForDates working first

        getDateRange().forEach(date -> {
            final Set<Route> loadedRoutes = getLoadedTramRoutes(date).collect(Collectors.toSet());
            KnownTramRoute.getFor(date).forEach(knownTramRoute -> {
                String prefix = "On " + date + " ";
                List<Route> findLoadedFor = loadedRoutes.stream().
                        filter(loadedRoute -> loadedRoute.getShortName().equals(knownTramRoute.shortName())).toList();
                assertEquals(1, findLoadedFor.size(), prefix + "could not find loaded route using short name match for " + knownTramRoute);
//                Route loadedRoute = findLoadedFor.get(0);
//                assertEquals(loadedRoute.getShortName(), knownTramRoute.shortName(), prefix + "short name incorrect for " + knownTramRoute);

            });
        });
    }

    @Test
    void shouldNotHaveUnknownTramRoutes() {
        TramDate start = TramDate.from(TestEnv.LocalNow());

        DateRange dateRange = DateRange.of(start, when.plusWeeks(6));

        Map<TramDate, IdSet<Route>> unknownForDate = new HashMap<>();

        dateRange.stream().forEach(date -> {
            final IdSet<Route> known = KnownTramRoute.getFor(date).stream().map(KnownTramRoute::getId).collect(IdSet.idCollector());
            final Set<Route> unknown = getLoadedTramRoutes(date).filter(route -> !known.contains(route.getId())).collect(Collectors.toSet());
            if (!unknown.isEmpty()) {
                unknownForDate.put(date, unknown.stream().collect(IdSet.collector()));
            }
        });

        assertTrue(unknownForDate.isEmpty(), "Unknown loaded routes " + unknownForDate);
    }

    @Test
    void shouldNotHaveUnusedKnownTramRoutesForDate() {
        TramDate start = TramDate.from(TestEnv.LocalNow());

        DateRange dateRange = DateRange.of(start, when.plusWeeks(6));

        SortedMap<TramDate, Set<KnownTramRoute>> unusedForDate = new TreeMap<>();

        dateRange.stream().
                forEach(date -> {
                    final IdSet<Route> loaded = getLoadedTramRoutes(date).collect(IdSet.collector());

                    final Set<KnownTramRoute> knownButUnused = KnownTramRoute.getFor(date).stream().
                            filter(knownTramRoute -> !loaded.contains(knownTramRoute.getId())).
                            collect(Collectors.toSet());
                    if (!knownButUnused.isEmpty()) {
                        unusedForDate.put(date, knownButUnused);
                    }
                });

        assertTrue(unusedForDate.isEmpty(), "For dates " + unusedForDate.keySet() + "\n Have unused loaded routes " + unusedForDate);
    }

    @NotNull
    private Stream<Route> getLoadedTramRoutes(TramDate date) {
        return routeRepository.getRoutesRunningOn(date).stream().filter(route -> route.getTransportMode() == Tram);
    }

}
