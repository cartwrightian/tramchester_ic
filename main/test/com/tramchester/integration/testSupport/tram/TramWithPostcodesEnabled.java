package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.RemoteDataSourceConfig;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class TramWithPostcodesEnabled extends IntegrationTramTestConfig {

    public TramWithPostcodesEnabled(IntegrationTramTestConfig.LiveData liveData, IntegrationTramTestConfig.Caching caching) {
        super(liveData, caching);
    }

    public TramWithPostcodesEnabled() {
        super(LiveData.Disabled, Caching.Enabled);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Arrays.asList(remoteTFGMConfig, postCodeDatasourceConfig);
    }

    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder();
    }
}
