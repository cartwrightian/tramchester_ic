package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class IntegrationTramTestConfigWithNaptan extends IntegrationTramTestConfig {

    private final EnumSet<TransportMode> transportModes;

    public IntegrationTramTestConfigWithNaptan(EnumSet<TransportMode> transportModes) {
        super();
        this.transportModes = transportModes;
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteTFGMConfig, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        if (transportModes.size()==1 && transportModes.contains(TransportMode.Tram)) {
            return super.getGraphDBConfig();
        }
        throw new RuntimeException("No supported for this config");
    }

    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder();
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return transportModes;
    }
}
