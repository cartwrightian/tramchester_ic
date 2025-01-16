package com.tramchester.integration.testSupport.config;

import com.tramchester.config.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.rail.OpenRailDataTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.rail.TestRailConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.NeighbourTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestOpenLdbConfig;
import com.tramchester.testSupport.reference.TramStations;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

// NOTE: if rename this take care of names in build.gradle, for example on integrationGM target
public class RailAndTramGreaterManchesterConfig extends IntegrationTramTestConfig {

    public RailAndTramGreaterManchesterConfig() {
        super(LiveData.Enabled, IntegrationTestConfig.CurrentClosures, IntegrationTestConfig.CurrentStationWalks, Caching.Enabled);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        // TODO temp removed remoteDBSourceConfig
        return Arrays.asList(railRemoteDataSource, remoteTFGMConfig, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public RailConfig getRailConfig() {
        return new TestRailConfig();
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return EnumSet.of(TransportMode.Tram, TransportMode.Train, TransportMode.RailReplacementBus);
    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getGreaterManchester();
    }

    @Override
    public int getNumberQueries() {
        return 3;
    }

    @Override
    public int getQueryInterval() {
        return 10;
    }

    @Override
    public boolean hasNeighbourConfig() {
        return true;
    }

    @Override
    public NeighbourConfig getNeighbourConfig() {
        NeighbourTestConfig config = new NeighbourTestConfig(0.2D, 3);
        config.addNeighbours(TramStations.EastDidsbury.getId(), RailStationIds.EastDidsbury.getId());
        config.addNeighbours(TramStations.Eccles.getId(), RailStationIds.Eccles.getId());
        config.addNeighbours(TramStations.Ashton.getId(), RailStationIds.Ashton.getId());
        return config;
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("tramTrainIntegration");
    }

    @Override
    public OpenLdbConfig getOpenldbwsConfig() {
        return new TestOpenLdbConfig();
    }

    @Override
    public OpenRailDataConfig getOpenRailDataConfig() {
        return new OpenRailDataTestConfig();
    }

}
