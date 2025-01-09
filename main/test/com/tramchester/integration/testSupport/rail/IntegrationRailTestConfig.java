package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.*;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.config.GraphDBTestConfig;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestOpenLdbConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IntegrationRailTestConfig extends IntegrationTestConfig {

    private static final TestGroupType testGroupType = TestGroupType.integration;
    private final boolean national;

//    public IntegrationRailTestConfig() {
//        this(false);
//    }

    public IntegrationRailTestConfig(boolean national) {
        super(testGroupType);
        this.national = national;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        String prefix = national ? "national" : "local";
        return new GraphDBTestConfig(prefix, testGroupType, this);
    }

    @Override
    public RailConfig getRailConfig() {
       return new TestRailConfig(railRemoteDataSource);
    }

    @Override
    public BoundingBox getBounds() {
        if (national) {
            return TestEnv.getNationalTrainBounds();
        } else {
            return TestEnv.getGreaterManchesterBounds();
        }
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
        String scope = national ? "national" : "local";
        return TestEnv.CACHE_DIR.resolve("railIntegration_"+scope);
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
