package com.tramchester.integration.testSupport.config;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.StationClosuresConfig;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceTestConfig remoteNaptanXMLConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    protected final RemoteDataSourceConfig remoteNPTGconfig;

    protected final RailRemoteDataSourceConfig railRemoteDataSource;

    private static final StationClosuresConfig shudehillClosed;
    private static final StationClosuresConfig marketStreetClosed;

    static {
        final LocalDate begin = LocalDate.of(2024, 7, 24);
        final LocalDate end = LocalDate.of(2024, 8, 19);
        shudehillClosed = new StationClosuresConfig(Collections.singleton("9400ZZMASHU"),
                begin, end, true, Collections.emptySet(), new HashSet<>(Arrays.asList("9400ZZMAVIC", "9400ZZMAEXS")));
        marketStreetClosed = new StationClosuresConfig(Collections.singleton("9400ZZMAMKT"),
                begin, end, true, Collections.emptySet(), new HashSet<>(Arrays.asList("9400ZZMAPGD", "9400ZZMASTP")));
    }

//    static {
//        StationPairConfig stationPair = new StationPairConfig("9400ZZMAPIC", "9400ZZMAPGD");
//        londonRoadClosure = new TemporaryStationsWalkIdsConfig(stationPair,
//                LocalDate.of(2024,6,22), LocalDate.of(2024, 7,9));
//    }

    public static final List<StationClosures> CurrentClosures = Arrays.asList(shudehillClosed, marketStreetClosed);
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
