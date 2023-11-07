package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.RemoteDataSourceConfig;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class IntegrationTramTestConfigWithNaptan extends IntegrationTramTestConfig {

    public IntegrationTramTestConfigWithNaptan() {
        super();
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteTFGMConfig, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder();
    }
}
