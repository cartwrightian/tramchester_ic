package com.tramchester.integration.graph.search;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.caching.LoaderSaverFactory;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.Route;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.InMemoryDataCache;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.MultiMode;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
public class RouteIndexTest extends EasyMockSupport {

    private static Path cacheFile;
    private static GuiceContainerDependencies componentContainer;
    private static Path otherFile;

    private RouteRepository routeRepository;
    private LoaderSaverFactory factory;
    private RouteIndex routeIndex;
    private TramRouteHelper routeHelper;
    private TramDate date;

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) throws IOException {
        final Path cacheFolder = tramchesterConfig.getCacheFolder();

        cacheFile = cacheFolder.resolve(RouteToRouteCosts.INDEX_FILE);
        otherFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".fortesting.json");

        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        Files.deleteIfExists(otherFile);

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        routeRepository = componentContainer.get(RouteRepository.class);
        factory = componentContainer.get(LoaderSaverFactory.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        routeHelper = new TramRouteHelper(componentContainer);
        date = TestEnv.testDay();

        Files.deleteIfExists(otherFile);
    }

    @AfterEach
    void onceAfterEachTestRuns() throws IOException {
        Files.deleteIfExists(otherFile);
    }

    @Test
    void shouldHaveMatchedContentInCacheFile() {

        assertTrue(cacheFile.toFile().exists(), "Missing " + cacheFile.toAbsolutePath());

        TransportDataFromFile<RouteIndexData> loader = factory.getDataLoaderFor(RouteIndexData.class, cacheFile);

        Stream<RouteIndexData> indexFromFile = loader.load();

        macthesRouteRepository(indexFromFile);
    }

    @Test
    void shouldHaveIndexForAllKnownRoutes() {

        int length = KnownTramRoute.getFor(date).size(); //KnownTramRoute.values().length;
        for (int i = 0; i < length; i++) {
            Route route = routeHelper.getPink(date);
            short index = routeIndex.indexFor(route.getId()); // throws on error

            Route result = routeIndex.getRouteFor(index);
            assertEquals(route.getId(), result.getId());
        }
    }

    @Test
    void shouldSaveToCacheAndReload() {

        RouteIndexPairFactory pairFactory = componentContainer.get(RouteIndexPairFactory.class);

        InMemoryDataCache dataCache = new InMemoryDataCache();
        RouteIndex local = new RouteIndex(routeRepository, new GraphFilterActive(false), dataCache, pairFactory);

        local.start();
        local.stop();

        assertTrue(dataCache.hasData(RouteIndex.RouteIndexes.class));

        Stream<RouteIndexData> indexFromFile = dataCache.getDataFor(RouteIndex.RouteIndexes.class);

        macthesRouteRepository(indexFromFile);

    }

    private void macthesRouteRepository(Stream<RouteIndexData> loaded) {
        List<RouteIndexData> resultsForIndex = loaded.toList();

        IdSet<Route> expected = routeRepository.getRoutes().stream().collect(IdSet.collector());

        assertEquals(expected.size(), resultsForIndex.size());

        IdSet<Route> idsFromIndex = resultsForIndex.stream().map(RouteIndexData::getRouteId).collect(IdSet.idCollector());
        assertEquals(expected, idsFromIndex);
    }


}
