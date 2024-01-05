package com.tramchester.integration.dataimport;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.DirectDataSourceFactory;
import com.tramchester.dataimport.loader.PopulateTransportDataFromSources;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.dataimport.loader.TransportDataSourceFactory;
import com.tramchester.dataimport.rail.RailTransportDataFromFiles;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Performance testing only")
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
    public void shouldDoMultipleLoadsForPerformanceTestingOnly() {

        NaptanRepository naptanRepository = componentContainer.get(NaptanRepository.class);
        UnzipFetchedData.Ready dataReady = componentContainer.get(UnzipFetchedData.class).getReady();
        ProvidesNow providesNow= componentContainer.get(ProvidesNow.class);
        TransportDataReaderFactory readerFactory = componentContainer.get(TransportDataReaderFactory.class);

        RailTransportDataFromFiles loadsRail = componentContainer.get(RailTransportDataFromFiles.class); // disabled for tram
        DirectDataSourceFactory directDataSourceFactory = new DirectDataSourceFactory(loadsRail);

        TransportDataSourceFactory transportDataSourceFactory = new TransportDataSourceFactory(readerFactory, naptanRepository, dataReady);

        for (int i = 0; i < 20; i++) {
            PopulateTransportDataFromSources populateTransportDataFromSources =
                    new PopulateTransportDataFromSources(transportDataSourceFactory, directDataSourceFactory, config, providesNow);
            transportDataSourceFactory.start();
            populateTransportDataFromSources.start();
            populateTransportDataFromSources.getData(); // load the data
            populateTransportDataFromSources.stop();
            transportDataSourceFactory.stop();
        }
    }

}
