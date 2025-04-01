package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.caching.DataCache;
import com.tramchester.caching.FileDataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RoutePairInterconnectsData;
import com.tramchester.domain.IdPair;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.search.routes.*;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.InMemoryDataCache;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
public class RouteInterconnectRepositoryTest {
    private static ComponentContainer componentContainer;

    private TramRouteHelper routeHelper;
    private TramDate date;
    private RouteCostMatrix routeMatrix;
    private RouteIndex routeIndex;
    private EnumSet<TransportMode> modes;
    private RouteRepository routeRepository;
    private InterchangeRepository interchangeRepository;
    private RouteInterconnectRepository repository;

    // NOTE: this test does not cause a full db rebuild, so might see VERSION node missing messages

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {

        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        ///// NOTE Clears the Cache

        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
        routeMatrix = componentContainer.get(RouteCostMatrix.class);
        repository = componentContainer.get(RouteInterconnectRepository.class);
        routeIndex = componentContainer.get(RouteIndex.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);

        date = TestEnv.testDay();
        modes = EnumSet.of(Tram);
    }

    @Test
    void shouldHaveExpectedInterchangeForSimpleInterchange() {
        Route routeA = routeHelper.getOneRoute(getBuryManchesterAltrincham(date), date);
        Route routeB = routeHelper.getOneRoute(getDeansgateManchesterAirport(date), date);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);

        assertNotEquals(0, dateOverlaps.numberOfBitsSet());

        PathResults results = repository.getInterchangesFor(indexPair, dateOverlaps, interchangeStation -> true);

        assertTrue(results.hasAny());

        assertEquals(4, results.numberPossible(), results.toString());
        assertEquals(1, results.getDepth());

    }

    @Test
    void shouldHaveExpectedInterchangeForSimpleInterchangeFiltered() {
        Route routeA = routeHelper.getOneRoute(getBuryManchesterAltrincham(date), date);
        Route routeB = routeHelper.getOneRoute(getDeansgateManchesterAirport(date), date);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);

        assertNotEquals(0, dateOverlaps.numberOfBitsSet());

        PathResults results = repository.getInterchangesFor(indexPair, dateOverlaps,
                interchangeStation -> interchangeStation.getStationId().equals(Victoria.getId()));

        assertTrue(results.hasAny());
        assertEquals(1, results.numberPossible(), results.toString());

        assertEquals(1, results.getDepth());

    }

    @Test
    void shouldHaveExpectedInterchangeForSimpleInterchangeNotOnDate() {

        // use date where we can get routes
        Route routeA = routeHelper.getOneRoute(getBuryManchesterAltrincham(date), date);
        Route routeB = routeHelper.getOneRoute(getDeansgateManchesterAirport(date), date);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        TramDate outOfRangeDate = TestEnv.testDay().plusWeeks(11 * 52);
        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(outOfRangeDate, modes);

        assertEquals(0, dateOverlaps.numberOfBitsSet());

        PathResults results = repository.getInterchangesFor(indexPair, dateOverlaps, interchangeStation -> true);

        assertFalse(results.hasAny());
        assertEquals(Integer.MAX_VALUE, results.getDepth());

        assertEquals(0, results.numberPossible());

    }

    @DisabledUntilDate(year = 2025, month = 4, day = 14)
    @Test
    void shouldCheckFor2Changes() {

        Route routeA = routeHelper.getOneRoute(getPiccadillyVictoria(date), date);
        Route routeB = routeHelper.getOneRoute(getCornbrookTheTraffordCentre(date), date);

        assertEquals(2, routeMatrix.getConnectionDepthFor(routeA, routeB));

        RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));

        // ignore data and mode here???
        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);
        // 196 -> 49
        assertEquals(49, dateOverlaps.numberOfBitsSet());

        PathResults results = repository.getInterchangesFor(indexPair, dateOverlaps, interchangeStation -> true);

        assertTrue(results.hasAny());

        PathResults.HasPathResults hasResults = (PathResults.HasPathResults) results;

        assertTrue(hasResults.hasAny());

        assertEquals(2, hasResults.getDepth()); // two sets of changes needed

        // changes frequently
        //assertEquals(4, hasResults.numberPossible(), hasResults.toString());
    }

    @Test
    void shouldHaveExpectedBacktrackFor1Changes() {
        Route routeA = routeHelper.getOneRoute(getBuryManchesterAltrincham(date), date);
        Route routeB = routeHelper.getOneRoute(getDeansgateManchesterAirport(date), date);
        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        assertTrue(interchangeRepository.hasInterchangeFor(indexPair));
        Set<InterchangeStation> interchanges = interchangeRepository.getInterchangesFor(indexPair).collect(Collectors.toSet());

        assertEquals(4, interchanges.size(), HasId.asIds(interchanges));

        // unrealistic as would be 0 in code, direct via one interchange
        assertEquals(1, routeMatrix.getConnectionDepthFor(routeA, routeB));

        Set<Pair<RoutePair, RoutePair>> results = repository.getBackTracksFor(1, indexPair);

        // all pairs should have interchanges
        Set<Pair<RouteIndexPair, RouteIndexPair>> noInterchanges = results.stream().
                map(pair -> Pair.of(routeIndex.getPairFor(pair.getLeft()), routeIndex.getPairFor(pair.getRight()))).
                filter(pair -> !(interchangeRepository.hasInterchangeFor(pair.getLeft()) && interchangeRepository.hasInterchangeFor(pair.getRight()))).
                collect(Collectors.toSet());

        assertTrue(noInterchanges.isEmpty(), noInterchanges.toString());

        Set<Route> wrongFirst = results.stream().map(pair -> pair.getLeft().first()).
                filter(first -> !first.equals(routeA)).
                collect(Collectors.toSet());

        assertTrue(wrongFirst.isEmpty(), wrongFirst.toString());

        Set<Route> wrongSecond = results.stream().map(pair -> pair.getRight().second()).
                filter(second -> !second.equals(routeB)).
                collect(Collectors.toSet());

        assertTrue(wrongSecond.isEmpty(), wrongFirst.toString());

    }

    @DisabledUntilDate(year = 2025, month = 4, day = 14)
    @Test
    void shouldHaveExpectedBacktrackFor2Changes() {
        Route routeA = routeHelper.getOneRoute(getPiccadillyVictoria(date), date);
        Route routeB = routeHelper.getOneRoute(getCornbrookTheTraffordCentre(date), date);
        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        assertFalse(interchangeRepository.hasInterchangeFor(indexPair));

        assertEquals(2, routeMatrix.getConnectionDepthFor(routeA, routeB));

        Set<Pair<RoutePair, RoutePair>> results = repository.getBackTracksFor(1, indexPair);

        // all pairs should have interchanges
        Set<Pair<RoutePair, RoutePair>> noInterchanges = results.stream().
                map(pair -> Pair.of(routeIndex.getPairFor(pair.getLeft()), routeIndex.getPairFor(pair.getRight()))).
                filter(pair -> !(interchangeRepository.hasInterchangeFor(pair.getLeft()) && interchangeRepository.hasInterchangeFor(pair.getRight()))).
                map(pair -> Pair.of(routeIndex.getPairFor(pair.getLeft()), routeIndex.getPairFor(pair.getRight()))).
                collect(Collectors.toSet());

        assertTrue(noInterchanges.isEmpty(), toString(noInterchanges));

        Set<Route> wrongFirst = results.stream().map(pair -> pair.getLeft().first()).
                filter(first -> !first.equals(routeA)).
                collect(Collectors.toSet());

        assertTrue(wrongFirst.isEmpty(), wrongFirst.toString());

        Set<Route> wrongSecond = results.stream().map(pair -> pair.getRight().second()).
                filter(second -> !second.equals(routeB)).
                collect(Collectors.toSet());

        assertTrue(wrongSecond.isEmpty(), wrongFirst.toString());

    }

    private String toString(Set<Pair<RoutePair, RoutePair>> pairs) {
        Set<Pair<IdPair<Route>, IdPair<Route>>> converted = pairs.stream().
                map(pair -> Pair.of(pair.getLeft().getIds(), pair.getRight().getIds())).
                collect(Collectors.toSet());
        return converted.toString();
    }

    @DisabledUntilDate(year = 2025, month = 4,  day = 14)
    @Test
    void shouldCheckFor2ChangesFiltered() {
        Route routeA = routeHelper.getOneRoute(getPiccadillyVictoria(date), date);
        Route routeB = routeHelper.getOneRoute(getCornbrookTheTraffordCentre(date), date);
        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);

        Function<InterchangeStation, Boolean> marketStreetOrCornbrook = interchangeStation -> interchangeStation.getStationId().equals(Cornbrook.getId()) ||
                interchangeStation.getStationId().equals(MarketStreet.getId());

        PathResults viaMarketStreetAndCornbook = repository.getInterchangesFor(indexPair, dateOverlaps, marketStreetOrCornbrook);

        assertTrue(viaMarketStreetAndCornbook.hasAny());

        assertInstanceOf(PathResults.HasPathResults.class, viaMarketStreetAndCornbook, "No path results");

        PathResults.HasPathResults results = (PathResults.HasPathResults) viaMarketStreetAndCornbook;
        assertNotNull(results);

        Set<QueryPathsWithDepth.BothOf> parts = results.stream().
                filter(path -> path instanceof QueryPathsWithDepth.BothOf).
                map(path -> (QueryPathsWithDepth.BothOf)path).collect(Collectors.toSet());

        assertFalse(parts.isEmpty());

        parts.forEach(part -> {
            QueryPathsWithDepth.QueryPath firstPath = part.getFirst();
            assertTrue(firstPath.isValid(interchangeStation -> interchangeStation.getStationId().equals(MarketStreet.getId())), part.toString());
            assertFalse(firstPath.isValid(interchangeStation -> interchangeStation.getStationId().equals(Cornbrook.getId())), part.toString());
            QueryPathsWithDepth.QueryPath secondPath = part.getSecond();
            assertTrue(secondPath.isValid(interchangeStation -> interchangeStation.getStationId().equals(Cornbrook.getId())), part.toString());
            assertFalse(secondPath.isValid(interchangeStation -> interchangeStation.getStationId().equals(MarketStreet.getId())), part.toString());
        });

    }

    @Test
    void shouldReproIssueWithGreenLineRoute() {
        RouteIndexPairFactory pairFactory = componentContainer.get(RouteIndexPairFactory.class);

        Route greenInbound = routeHelper.getOneRoute(getBuryManchesterAltrincham(date), date);

        short greenIndex = routeIndex.indexFor(greenInbound.getId());

        Set<Route> routes = routeRepository.getRoutes(TramsOnly).stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        int numberOfRoutes = routeRepository.numberOfRoutes();
        IndexedBitSet dateOverlaps = IndexedBitSet.getIdentity(numberOfRoutes, numberOfRoutes);

        for(Route otherRoute : routes) {
            if (!otherRoute.getId().equals(greenInbound.getId())) {
                short otherIndex = routeIndex.indexFor(otherRoute.getId());

                RouteIndexPair routeIndexPair = pairFactory.get(greenIndex, otherIndex);
                PathResults results = repository.getInterchangesFor(routeIndexPair, dateOverlaps, interchangeStation -> true);

                assertTrue(results.hasAny(), "no link for " + greenInbound + " and " + otherRoute);
            }
        }

    }

    @Test
    void shouldCacheValuesInMemory() {
        InMemoryDataCache dataCache = new InMemoryDataCache();

        RouteInterconnectRepository repository = createRepositoryForTestingWith(dataCache);

        repository.start();
        int count = repository.getNumberBacktrackFor(1);
        repository.stop();

        assertTrue(dataCache.hasData(RouteInterconnectRepository.RouteInterconnects.class));

        List<RoutePairInterconnectsData> fromCache = dataCache.getDataFor(RouteInterconnectRepository.RouteInterconnects.class).toList();

        assertFalse(fromCache.isEmpty());

        long computed = fromCache.stream().filter(item -> item.getDepth() == 0).count();
        assertEquals(count, Math.toIntExact(computed));

        repository.start();
        int countAfterCache = repository.getNumberBacktrackFor(1);
        repository.stop();

        assertEquals(count, countAfterCache);
    }

    @Test
    void shouldCacheValuesFile() {
        FileDataCache dataCache = componentContainer.get(FileDataCache.class);

        RouteInterconnectRepository repository = createRepositoryForTestingWith(dataCache);

        repository.start();
        int count = repository.getNumberBacktrackFor(1);
        repository.stop();

        repository.start();
        int countAfterCache = repository.getNumberBacktrackFor(1);
        repository.stop();

        assertEquals(count, countAfterCache);
    }

    @NotNull
    private RouteInterconnectRepository createRepositoryForTestingWith(DataCache dataCache) {
        RouteIndexPairFactory pairFactory = componentContainer.get(RouteIndexPairFactory.class);
        NumberOfRoutes numberOfRoutes = componentContainer.get(NumberOfRoutes.class);
        RouteDateAndDayOverlap routeDateAndDayOverlap = componentContainer.get(RouteDateAndDayOverlap.class);

        return new RouteInterconnectRepository(pairFactory, numberOfRoutes, routeIndex, interchangeRepository,
                routeMatrix, routeDateAndDayOverlap, dataCache, new GraphFilterActive(false));
    }


}
