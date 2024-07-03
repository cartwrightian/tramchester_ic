package com.tramchester.integration.testSupport.config;

import com.tramchester.config.*;
import com.tramchester.domain.StationClosures;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceTestConfig;
import com.tramchester.integration.testSupport.nptg.NPTGDataSourceTestConfig;
import com.tramchester.integration.testSupport.postcodes.PostCodeDatasourceConfig;
import com.tramchester.integration.testSupport.rail.RailRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceTestConfig remoteNaptanXMLConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    protected final RemoteDataSourceConfig remoteNPTGconfig;

    protected final RailRemoteDataSourceConfig railRemoteDataSource;

//    static {
//        Set<String> closed = Collections.singleton("9400ZZMAEXS");
//        exchangeSquareBrokenRail = new StationClosuresConfig(closed, LocalDate.of(2024,2,12),
//            LocalDate.of(2024,5,7), false, Collections.singleton("9400ZZMAVIC"));
//    }

    private static final TemporaryStationsWalkIdsConfig londonRoadClosure;

    static {
        StationPairConfig stationPair = new StationPairConfig("9400ZZMAPIC", "9400ZZMAPGD");
        londonRoadClosure = new TemporaryStationsWalkIdsConfig(stationPair,
                LocalDate.of(2024,6,22), LocalDate.of(2024, 7,9));
    }

    public static final List<StationClosures> CurrentClosures = Collections.emptyList();
    public static final List<TemporaryStationsWalkIds> CurrentStationWalks = List.of(londonRoadClosure);

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
