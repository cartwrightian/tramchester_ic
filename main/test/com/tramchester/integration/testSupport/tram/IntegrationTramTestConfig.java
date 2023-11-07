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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class IntegrationTramTestConfig extends IntegrationTestConfig {

    public enum LiveData {
        Enabled, Disabled;
    }

    public enum Caching {
        Enabled, Disabled
    }

    private static final String DB_NAME = "int_test_tram.db";
    public static final Path TRAM_INTEGRATION_CACHE_PATH = TestEnv.CACHE_DIR.resolve("tramIntegration");

    private final GTFSSourceConfig gtfsSourceConfig;
    protected final RemoteDataSourceConfig remoteTFGMConfig;
    protected final RemoteDataSourceConfig remoteDBSourceConfig;
    private final LiveData liveData;
    private final Caching caching;

    public IntegrationTramTestConfig() {
       this(DB_NAME, LiveData.Disabled, IntegrationTestConfig.CurrentClosures, Caching.Enabled);
    }

    public IntegrationTramTestConfig(LiveData liveData) {
        this(DB_NAME, liveData, IntegrationTestConfig.CurrentClosures, Caching.Enabled);
    }

    public IntegrationTramTestConfig(LiveData liveData, Caching caching) {
        this(DB_NAME, liveData, IntegrationTestConfig.CurrentClosures, caching);
    }

    public IntegrationTramTestConfig(String dbName, List<StationClosures> closedStations) {
        this(dbName, LiveData.Disabled, closedStations, Caching.Enabled);
    }

    public IntegrationTramTestConfig(String dbName, List<StationClosures> closedStations, Caching caching) {
        this(dbName, LiveData.Disabled, closedStations, caching);
    }

    private IntegrationTramTestConfig(String dbName, LiveData liveData, List<StationClosures> closedStations, Caching caching) {
        this(new GraphDBIntegrationTramTestConfig("integrationTramTest", dbName), liveData, closedStations, caching);
    }

    protected IntegrationTramTestConfig(GraphDBTestConfig dbTestConfig, LiveData liveData, List<StationClosures> closedStations,
                                        Caching caching) {
        super(dbTestConfig);
        this.liveData = liveData;
        this.caching = caching;

        Path downloadFolder = Path.of("data/tram");
        gtfsSourceConfig = new TFGMGTFSSourceTestConfig(downloadFolder, GTFSTransportationType.tram,
                TransportMode.Tram, AdditionalTramInterchanges.stations(), Collections.emptySet(), closedStations,
                Duration.ofMinutes(13));
        remoteTFGMConfig = TFGMRemoteDataSourceConfig.createFor(downloadFolder);
        remoteDBSourceConfig = new DatabaseRemoteDataSourceConfig(Path.of("databases"));

        if (this.caching==Caching.Enabled) {
            if (!this.getClass().equals(IntegrationTramTestConfig.class)) {
                Optional<Method> foundOverride = Arrays.stream(this.getClass().getDeclaredMethods()).
                        filter(method -> method.getName().equals("getCacheFolder")).
                        filter(method -> method.getParameterCount() == 0).
                        findAny();

                if (foundOverride.isEmpty()) {
                    throw new RuntimeException("getCacheFolder must be override for " + this.getClass().getSimpleName());
                }
            }
        }
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
        if (liveData ==LiveData.Enabled) {
            return new TestTramLiveDataConfig();
        }
        return null;
    }

    private static class GraphDBIntegrationTramTestConfig extends GraphDBTestConfig {
        public GraphDBIntegrationTramTestConfig(String folder, String dbFilename) {
            super(folder, dbFilename);
        }
    }

    @Override
    public Path getCacheFolder() {
        return TRAM_INTEGRATION_CACHE_PATH;
    }

    @Override
    public boolean getCachingDisabled() {
        return caching==Caching.Disabled;
    }

}

