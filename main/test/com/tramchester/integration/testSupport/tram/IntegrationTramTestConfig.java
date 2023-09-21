package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.DatabaseRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestTramLiveDataConfig;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntegrationTramTestConfig extends IntegrationTestConfig {

    private static final String DB_NAME = "int_test_tram.db";

    private final GTFSSourceConfig gtfsSourceConfig;
    protected final RemoteDataSourceConfig remoteTFGMConfig;
    protected final RemoteDataSourceConfig remoteDBSourceConfig;
    private final boolean liveDataEnabled;

    public IntegrationTramTestConfig() {
       this(DB_NAME, false, IntegrationTestConfig.CurrentClosures);
    }

    public IntegrationTramTestConfig(boolean liveDataEnabled) {
        this(DB_NAME, liveDataEnabled, IntegrationTestConfig.CurrentClosures);
    }

    public IntegrationTramTestConfig(String dbName, List<StationClosures> closedStations) {
        this(dbName, false, closedStations);
    }

    private IntegrationTramTestConfig(String dbName, boolean liveDataEnabled, List<StationClosures> closedStations) {
        this(new GraphDBIntegrationTramTestConfig("integrationTramTest", dbName), liveDataEnabled, closedStations);
    }

    protected IntegrationTramTestConfig(GraphDBTestConfig dbTestConfig, boolean liveDataEnabled, List<StationClosures> closedStations) {
        super(dbTestConfig);
        this.liveDataEnabled = liveDataEnabled;
        gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/tram", GTFSTransportationType.tram,
                TransportMode.Tram, AdditionalTramInterchanges.stations(), Collections.emptySet(), closedStations,
                Duration.ofMinutes(13));
        remoteTFGMConfig = new TFGMRemoteDataSourceConfig("data/tram");
        remoteDBSourceConfig = new DatabaseRemoteDataSourceConfig(Path.of("databases"));
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(gtfsSourceConfig);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteTFGMConfig, remoteDBSourceConfig); // naptan disabled for trams
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 6;
    }

    @Override
    public TfgmTramLiveDataConfig getLiveDataConfig() {
        if (liveDataEnabled) {
            return new TestTramLiveDataConfig();
        }
        return null;
    }

    private static class GraphDBIntegrationTramTestConfig extends GraphDBTestConfig {
        public GraphDBIntegrationTramTestConfig(String folder, String dbFilename) {
            super(folder, dbFilename);
        }

//        @Override
//        public String getNeo4jPagecacheMemory() {
//            return "100m";
//        }
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("tramIntegration");
    }
}

