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

    // use StationClosuresConfigForTest to create closures here

//    private static final StationClosures rochdaleTownCentre = new StationClosuresConfigForTest(TramStations.Rochdale,
//            DateRange.of(TramDate.of(2024, 10, 19), TramDate.of(2024, 10, 31)),
//            true, Collections.emptySet(), Collections.singleton(TramStations.RochdaleRail)
//            );

    public static final List<StationClosures> CurrentClosures = Collections.emptyList();
    public static final List<TemporaryStationsWalkIds> CurrentStationWalks = Collections.emptyList();

    private final TestGroupType testGroupType;

    protected IntegrationTestConfig(TestGroupType testGroupType) {
        this.testGroupType = testGroupType;
        final Path naptanLocalDataPath = Path.of("data/naptan");
        remoteNaptanXMLConfig = new NaptanRemoteDataSourceTestConfig(naptanLocalDataPath);
        remoteNPTGconfig = new NPTGDataSourceTestConfig();
        postCodeDatasourceConfig = new PostCodeDatasourceConfig();
        railRemoteDataSource = new RailRemoteDataSourceConfig("data/rail");
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return new GraphDBTestConfig(testGroupType, this);
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
