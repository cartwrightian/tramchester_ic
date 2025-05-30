package com.tramchester.integration.testSupport.bus;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IntegrationBusTestConfig extends IntegrationTestConfig {
    private final GTFSSourceConfig gtfsSourceConfig;
    private final RemoteDataSourceConfig remoteDataSourceConfig;

    public IntegrationBusTestConfig() {
        this(TestGroupType.integration);
    }

    public IntegrationBusTestConfig(TestGroupType testGroupType) {
        super(testGroupType);

        final Set<TransportMode> modesWithPlatforms = Collections.emptySet();
        final IdSet<Station> additionalInterchanges = IdSet.emptySet();
        final Set<TransportMode> groupedStationModes = Collections.singleton(TransportMode.Bus);

        final Path downloadFolder = Path.of("data/bus");

        // TODO not long enough?
        Duration maxInitialWait = Duration.ofMinutes(45);
        gtfsSourceConfig = new TFGMGTFSSourceTestConfig(
                Collections.singleton(GTFSTransportationType.bus),
                modesWithPlatforms, additionalInterchanges, groupedStationModes, Collections.emptyList(), maxInitialWait,
                Collections.emptyList());
        remoteDataSourceConfig = TFGMRemoteDataSourceConfig.createFor(downloadFolder);
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(gtfsSourceConfig);
    }

    @Override
    public int getNumberQueries() { return 3; }

    @Override
    public boolean getDepthFirst() {
        return false;
    }

    @Override
    public int getQueryInterval() {
        return 20;
    }

    @Override
    public int getMaxWait() {
        return 45;
    }

    @Override
    public int getNumOfNearestStopsForWalking() {
        return 50;
    }

    @Override
    public Double getNearestStopRangeKM() {
        return 1.0D;
    }

    @Override
    public Double getNearestStopForWalkingRangeKM() {
        return 0.5D;
    }

    @Override
    public int getMaxJourneyDuration() {
        return 180;
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        // remove postCodeDatasourceConfig for now
        return Arrays.asList(remoteDataSourceConfig, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("busIntegration");
    }

}
