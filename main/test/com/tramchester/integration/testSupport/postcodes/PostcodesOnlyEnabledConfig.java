package com.tramchester.integration.testSupport.postcodes;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class PostcodesOnlyEnabledConfig extends IntegrationTramTestConfig {

    public PostcodesOnlyEnabledConfig() {
        super(LiveData.Disabled);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return List.of(postCodeDatasourceConfig);
    }

    @Override
    public List<GTFSSourceConfig> getGTFSDataSource() {
        return Collections.emptyList();
    }

    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder();
    }
}
