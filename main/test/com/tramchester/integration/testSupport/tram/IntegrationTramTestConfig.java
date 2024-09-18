package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.config.DatabaseRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TestGroupType;
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

    public static final Duration MAX_INITIAL_WAIT = Duration.ofMinutes(13);


    public enum LiveData {
        Enabled, Disabled, EnabledWithSNS
    }

    public enum Caching {
        Enabled, Disabled
    }

//    private static final String DB_NAME = "int_test_tram.db";
    public static final Path TRAM_INTEGRATION_CACHE_PATH = TestEnv.CACHE_DIR.resolve("tramIntegration");

    private final GTFSSourceConfig gtfsSourceConfig;
    protected final RemoteDataSourceConfig remoteTFGMConfig;
    protected final RemoteDataSourceConfig remoteDBSourceConfig;
    private final LiveData liveData;
    private final Caching caching;

    public IntegrationTramTestConfig() {
       this(LiveData.Disabled, IntegrationTestConfig.CurrentClosures, IntegrationTestConfig.CurrentStationWalks, Caching.Enabled);
    }

    public IntegrationTramTestConfig(LiveData liveData) {
        this(liveData, IntegrationTestConfig.CurrentClosures, IntegrationTestConfig.CurrentStationWalks, Caching.Enabled);
    }

    public IntegrationTramTestConfig(LiveData liveData, Caching caching) {
        this(liveData, IntegrationTestConfig.CurrentClosures, IntegrationTestConfig.CurrentStationWalks, caching);
    }

    public IntegrationTramTestConfig(final List<StationClosures> closedStations) {
        this(LiveData.Disabled, closedStations, IntegrationTestConfig.CurrentStationWalks, Caching.Enabled);
    }

    IntegrationTramTestConfig(List<StationClosures> closedStations, Caching caching) {
        this(LiveData.Disabled, closedStations, IntegrationTestConfig.CurrentStationWalks, caching);
        if (isGraphFiltered() && this.caching!=Caching.Disabled) {
            throw new RuntimeException("Misconfiguration, caching must be disabled if graph filtering is enabled");
        }
    }

    protected IntegrationTramTestConfig(LiveData liveData, List<StationClosures> closedStations, List<TemporaryStationsWalkIds> tempWalks, Caching caching) {
        super(TestGroupType.integration);
        this.liveData = liveData;
        this.caching = caching;

        Path downloadFolder = Path.of("data/tram");
        gtfsSourceConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
                TransportMode.Tram, AdditionalTramInterchanges.stations(), Collections.emptySet(), closedStations,
                MAX_INITIAL_WAIT, tempWalks);
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
        if (isGraphFiltered()) {
            return Collections.singletonList(remoteTFGMConfig);
        } else {
            return Arrays.asList(remoteTFGMConfig, remoteDBSourceConfig);
        }
    }

    @Override
    public int getNumberQueries() { return 1; }

    @Override
    public int getQueryInterval() {
        return 6;
    }

    @Override
    public TfgmTramLiveDataConfig getLiveDataConfig() {
        switch (liveData) {
            case Enabled -> {
                return new TestTramLiveDataConfig();
            }
            case Disabled -> {
                return null;
            }
            case EnabledWithSNS -> {
                return new TestTramLiveDataConfig(TestEnv.TEST_SNS_TOPIC_PREFIX);
            }
            default -> throw new RuntimeException("Unknown live data config " + liveData);
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

