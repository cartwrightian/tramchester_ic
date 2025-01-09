package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.*;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.config.GraphDBTestConfig;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestOpenLdbConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IntegrationRailTestConfig extends IntegrationTestConfig {

    private static final TestGroupType testGroupType = TestGroupType.integration;

    public enum Scope {
        National,
        GreaterManchester;
    }

    private final Scope geoScope;

    public IntegrationRailTestConfig(final Scope geoScope) {
        super(testGroupType);
        this.geoScope = geoScope;
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return List.of(railRemoteDataSource, remoteNaptanXMLConfig, remoteNPTGconfig);
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
//        if (geoScope==Scope.National) {
//            throw new RuntimeException("Wrong geo scope");
//        }
        return new GraphDBTestConfig(geoScope.name(), testGroupType, this);
    }

    @Override
    public Path getCacheFolder() {
        return TestEnv.CACHE_DIR.resolve("railIntegration_"+geoScope.name());
    }

    @Override
    public RailConfig getRailConfig() {
       return new TestRailConfig(railRemoteDataSource);
    }

    @Override
    public BoundingBox getBounds() {
        if (geoScope ==Scope.National) {
            return TestEnv.getNationalTrainBounds();
        } else if (geoScope ==Scope.GreaterManchester){
            return TestEnv.getGreaterManchesterBounds();
        } else {
            throw new RuntimeException("Unknown scope " + geoScope);
        }
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.emptyList();
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
