package com.tramchester.integration.testSupport.config;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RailConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.rail.RailRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.rail.TestRailConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Disabled;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.*;

@Disabled("All mode planning is WIP")
public class AllModesTestConfig extends IntegrationTestConfig {

    private TFGMRemoteDataSourceConfig remoteTfgmSourceConfig;
    private RailRemoteDataSourceConfig remoteDataRailConfig;

    public AllModesTestConfig() {
        super(TestGroupType.integration);
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        final Set<TransportMode> modesWithPlatforms = new HashSet<>(Arrays.asList(Tram, Train));
        final Set<TransportMode> compositeStationModes = Collections.singleton(Bus);

        Path dowloadFolder = Path.of("data/bus");
        final TFGMGTFSSourceTestConfig tfgmDataSource = new TFGMGTFSSourceTestConfig(TestEnv.tramAndBus,
                modesWithPlatforms, AdditionalTramInterchanges.stations(), compositeStationModes, Collections.emptyList(),
                Duration.ofMinutes(13), Collections.emptyList());

        remoteTfgmSourceConfig = TFGMRemoteDataSourceConfig.createFor(dowloadFolder);
        remoteDataRailConfig = new RailRemoteDataSourceConfig("data/openRailData");

        return List.of(tfgmDataSource);
    }

    @Override
    public RailConfig getRailConfig() {
        return new TestRailConfig();
    }


    @Override
    public BoundingBox getBounds() {
        return TestEnv.getGreaterManchesterBounds();
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteDataRailConfig, remoteNaptanXMLConfig, remoteTfgmSourceConfig, remoteNPTGconfig);
    }

    @Override
    public boolean hasNeighbourConfig() {
        return true;
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("allModes");
    }

}
