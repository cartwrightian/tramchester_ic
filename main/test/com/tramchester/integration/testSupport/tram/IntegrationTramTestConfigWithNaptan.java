package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class IntegrationTramTestConfigWithNaptan extends IntegrationTramTestConfig {

    private final ImmutableEnumSet<TransportMode> transportModes;

    public IntegrationTramTestConfigWithNaptan(ImmutableEnumSet<TransportMode> transportModes) {
        super();
        this.transportModes = transportModes;
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteSources() {
        return Arrays.asList(remoteTFGMConfig, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        if ((transportModes.size()==1 || transportModes.size()==2) && transportModes.contains(TransportMode.Tram)) {
            return super.getGraphDBConfig();
        }
        throw new RuntimeException("Not supported for this config " + transportModes);
    }

    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder();
    }

    @Override
    public ImmutableEnumSet<TransportMode> getTransportModesImmutable() {
        return transportModes;
    }
}
