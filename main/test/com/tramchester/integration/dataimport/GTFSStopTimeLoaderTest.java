package com.tramchester.integration.dataimport;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.*;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.repository.TransportDataFromFilesTramTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GTFSStopTimeLoaderTest {

    private static IntegrationTramTestConfig config;
    private static GuiceContainerDependencies componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldLoadStopDataAsExpected() {
        ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);
        TransportDataSourceFactory dataSourceFactory = componentContainer.get(TransportDataSourceFactory.class);

        TransportDataSource dataSource = dataSourceFactory.getFor(DataSourceID.tfgm);
        TransportEntityFactory entityFactory = dataSource.getEntityFactory() ;

        TransportDataContainer buildable = new TransportDataContainer(providesNow, "testSourceName");

        // agencies
        AgencyDataLoader agencyDataLoader = new AgencyDataLoader(dataSource.getDataSourceInfo(), entityFactory);
        CompositeIdMap<Agency, MutableAgency> interimAgencies = agencyDataLoader.load(dataSource.getAgencies());

        // routes
        RouteDataLoader routeDataLoader = new RouteDataLoader(buildable, dataSource.getConfig(), entityFactory);
        RouteDataLoader.ExcludedRoutes excludedRoutes = routeDataLoader.load(dataSource.getRoutes(), interimAgencies);

        // trips
        TripLoader tripLoader = new TripLoader(buildable, entityFactory);
        //RouteDataLoader.ExcludedRoutes excludedRoutes = new RouteDataLoader.ExcludedRoutes();
        PreloadTripAndServices tripsAndServices = tripLoader.load(dataSource.getTrips(), excludedRoutes);

        // stops
        StopDataLoader stopDataLoader = new StopDataLoader(entityFactory, config);
        PreloadedStationsAndPlatforms preloaded = stopDataLoader.load(dataSource.getStops());

        // stop times
        GTFSStopTimeLoader loader = new GTFSStopTimeLoader(buildable, entityFactory, dataSource.getConfig());
        loader.load(dataSource.getStopTimes(), preloaded, tripsAndServices);

        assertTrue(buildable.hasStationId(TramStations.MarketStreet.getId()));
        assertEquals(TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS, buildable.getStations().size());
        assertEquals(TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS,buildable.getStations(EnumSet.of(TransportMode.Tram)).size());

    }

    @Disabled("perf testing only")
    @Test
    public void shouldDoMultipleLoadsForPerformanceTestingOnly() {

        ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);
        TransportDataReaderFactory readerFactory = componentContainer.get(TransportDataReaderFactory.class);
        NaptanRepository naptanRepository = componentContainer.get(NaptanRepository.class);
        UnzipFetchedData unzipFetchedData = componentContainer.get(UnzipFetchedData.class);

        UnzipFetchedData.Ready ready = unzipFetchedData.getReady();
        TransportDataSourceFactory dataSourceFactory = new TransportDataSourceFactory(readerFactory, naptanRepository, ready);
        TransportDataContainer buildable = new TransportDataContainer(providesNow, "testSourceName");


        dataSourceFactory.start();
        TransportDataSource dataSource = dataSourceFactory.getFor(DataSourceID.tfgm);

        TransportEntityFactory entityFactory = dataSource.getEntityFactory() ;

        AgencyDataLoader agencyDataLoader = new AgencyDataLoader(dataSource.getDataSourceInfo(), entityFactory);
        CompositeIdMap<Agency, MutableAgency> interimAgencies = agencyDataLoader.load(dataSource.getAgencies());

        RouteDataLoader routeDataLoader = new RouteDataLoader(buildable, dataSource.getConfig(), entityFactory);
        RouteDataLoader.ExcludedRoutes excludedRoutes = routeDataLoader.load(dataSource.getRoutes(), interimAgencies);

        TripLoader tripLoader = new TripLoader(buildable, entityFactory);
        PreloadTripAndServices tripsAndServices = tripLoader.load(dataSource.getTrips(), excludedRoutes);

        StopDataLoader stopDataLoader = new StopDataLoader(entityFactory, config);
        PreloadedStationsAndPlatforms preloaded = stopDataLoader.load(dataSource.getStops());
        dataSourceFactory.stop();

        // stop times are the slowest by far
        for (int i = 0; i < 50; i++) {
            buildable.dispose();
            dataSourceFactory.start();
            dataSource = dataSourceFactory.getFor(DataSourceID.tfgm);
            entityFactory = dataSource.getEntityFactory() ;

            GTFSStopTimeLoader loader = new GTFSStopTimeLoader(buildable, entityFactory, dataSource.getConfig());
            loader.load(dataSource.getStopTimes(), preloaded, tripsAndServices);

            dataSourceFactory.stop();
        }

    }

}
