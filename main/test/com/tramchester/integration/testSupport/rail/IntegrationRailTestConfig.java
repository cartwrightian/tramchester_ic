package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.*;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestOpenLdbConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IntegrationRailTestConfig extends IntegrationTestConfig {

    public IntegrationRailTestConfig() {
        super(TestGroupType.integration);
    }

    @Override
    public RailConfig getRailConfig() {
       return new TestRailConfig(railRemoteDataSource);
    }

    @Override
    public BoundingBox getBounds() {
        return TestEnv.getTrainBounds();
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.emptyList();
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return List.of(railRemoteDataSource, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public int getMaxJourneyDuration() {
        return 820;
    } // 13.5 hours Aberdeen to Penzance is ~810 minutes

    @Override
    public int getMaxWait() {
        return 30;
    }

    @Override
    public int getQueryInterval() { return 20; }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("railIntegration");
    }

    @Override
    public OpenLdbConfig getOpenldbwsConfig() {
        return new TestOpenLdbConfig();
    }

    @Override
    public OpenRailDataConfig getOpenRailDataConfig() {
        return new OpenRailDataConfig() {
            @Override
            public String getUsername() {
                return System.getenv("OPENRAILDATA_USERNAME");
            }

            @Override
            public String getPassword() {
                return System.getenv("OPENRAILDATA_PASSWORD");
            }

            @Override
            public String getAuthURL() {
                return "https://opendata.nationalrail.co.uk/authenticate";
            }
        };
    }
}
