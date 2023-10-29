package com.tramchester.unit.graph.calculation;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;

public class SimpleGraphConfig extends IntegrationTestConfig {

    public SimpleGraphConfig(String dbFilename) {
        super(new GraphDBTestConfig("unitTest", dbFilename));
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig(Path.of("data/tram"),
                GTFSTransportationType.tram, Tram, IdSet.emptySet(), Collections.emptySet(), Collections.emptyList(),
                Duration.ofMinutes(13));
        return Collections.singletonList(tfgmTestDataSourceConfig);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Collections.emptyList();
    }

    @Override
    public int getNumberQueries() {
        return 1;
    }

    @Override
    public int getQueryInterval() {
        return 6;
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("unit");
    }

}
