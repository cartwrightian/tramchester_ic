package com.tramchester.integration.testSupport.config;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.StationClosures;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceTestConfig;
import com.tramchester.integration.testSupport.nptg.NPTGDataSourceTestConfig;
import com.tramchester.integration.testSupport.postcodes.PostCodeDatasourceConfig;
import com.tramchester.integration.testSupport.rail.RailRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceTestConfig remoteNaptanXMLConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    protected final RemoteDataSourceConfig remoteNPTGconfig;

    protected final RailRemoteDataSourceConfig railRemoteDataSource;

    public static final List<StationClosures> CurrentClosures = Collections.emptyList();

    /**
     * examples
     * closures
     *       - stations:
     *             ids: [ "9400ZZMAPGD" ]
     *         dateRange:
     *           begin: 2025-06-03
     *           end: 2025-08-10
     *         fullyClosed: true
     *         diversionsAroundClosure: [ ]
     *         diversionsToFromClosure: [ ]
     *
     * walks
     *       - stations:
     *           first: 9400ZZMAPIC
     *           second: 9400ZZMAMKT
     *         begin: 2025-06-03
     *         end: 2025-08-10
     */

    public static final List<TemporaryStationsWalkIds> CurrentStationWalks = Collections.emptyList();

    private final GraphDBTestConfig dbConfig;

    protected IntegrationTestConfig(TestGroupType testGroupType) {
        final Path naptanLocalDataPath = Path.of("data/naptan");
        remoteNaptanXMLConfig = new NaptanRemoteDataSourceTestConfig(naptanLocalDataPath);
        remoteNPTGconfig = new NPTGDataSourceTestConfig();
        postCodeDatasourceConfig = new PostCodeDatasourceConfig();
        railRemoteDataSource = new RailRemoteDataSourceConfig("data/openRailData");
        dbConfig = new GraphDBTestConfig(testGroupType, this);
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return dbConfig;
    }

    @Override
    public String getLiveDataSNSPublishTopic() {
        String text = super.getLiveDataSNSPublishTopic();

        if (TestEnv.isCircleci()) {
            return text.replace("Dev", "CI");
        }

        return text;
    }

    @Override
    public boolean getDepthFirst() {
        return true;
    }
}
