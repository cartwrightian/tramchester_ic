package com.tramchester.integration.mappers.serialisation;

import com.tramchester.caching.LoaderSaverFactory;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.integration.testSupport.rail.RailStationIds.LondonEuston;
import static com.tramchester.integration.testSupport.rail.RailStationIds.StokeOnTrent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteIndexDataSerialisationTest {

    private Path pathToJsonFile;
    private LoaderSaverFactory factory;

    @BeforeEach
    void onceBeforeEachTest() throws IOException {
        pathToJsonFile = TestEnv.getTempDir().resolve("testfile.json");
        Files.deleteIfExists(pathToJsonFile);
        factory = new LoaderSaverFactory();
        factory.start();
    }

    @Test
    void shouldSaveAndLoadToFileRouteId() throws Exception {

        IdFor<Route> routeId = Route.createBasicRouteId("routeB");
        RouteIndexData routeIndexData = new RouteIndexData((short) 42, routeId);

        saveToFile(routeIndexData);

        assertTrue(Files.exists(pathToJsonFile));

        List<RouteIndexData> loadedData = loadFromFile();

        assertEquals(1, loadedData.size());

        RouteIndexData loadedRouteIndexData = loadedData.getFirst();

        assertEquals(routeId, loadedRouteIndexData.getRouteId());
        assertEquals(42, loadedRouteIndexData.getIndex());

    }

    @Test
    void shouldSaveAndLoadToFileRailRouteId() throws Exception {

        RailRouteId railRouteId = getRailRouteId();

        RouteIndexData routeIndexData = new RouteIndexData((short) 42, railRouteId);

        saveToFile(routeIndexData);

        assertTrue(Files.exists(pathToJsonFile));

        List<RouteIndexData> loadedData = loadFromFile();

        assertEquals(1, loadedData.size());

        RouteIndexData loadedRouteIndexData = loadedData.getFirst();

        assertEquals(railRouteId, loadedRouteIndexData.getRouteId());
        assertEquals(42, loadedRouteIndexData.getIndex());

    }

    @NotNull
    private RailRouteId getRailRouteId() {
        IdFor<Agency> agencyId = Agency.createId("NT");
        return new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), agencyId, 1);
    }

    @NotNull
    private List<RouteIndexData> loadFromFile() {
        TransportDataFromFile<RouteIndexData> loader = factory.getDataLoaderFor(RouteIndexData.class, pathToJsonFile);

        Stream<RouteIndexData> stream = loader.load();

        return stream.collect(Collectors.toList());
    }

    private void saveToFile(RouteIndexData routeIndexData) {
        HasDataSaver<RouteIndexData> hasSaver = factory.getSaverFor(RouteIndexData.class, pathToJsonFile);

        try (HasDataSaver.ClosableDataSaver<RouteIndexData> saver = hasSaver.get()) {
            saver.write(routeIndexData);
        }

    }

}
