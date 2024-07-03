package com.tramchester.testSupport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.config.GraphDBTestConfig;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;

public class UnitTestOfGraphConfig extends TestConfig {

    public UnitTestOfGraphConfig() {
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig(
                EnumSet.of(GTFSTransportationType.tram),
                EnumSet.of(Tram), IdSet.emptySet(), EnumSet.noneOf(TransportMode.class), Collections.emptyList(),
                Duration.ofMinutes(13), Collections.emptyList());
        return Collections.singletonList(tfgmTestDataSourceConfig);
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

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return new GraphDBTestConfig(TestGroupType.unit, this);
    }

    @Override
    public boolean getDepthFirst() {
        return true;
    }
}
