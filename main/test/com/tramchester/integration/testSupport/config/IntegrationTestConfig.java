package com.tramchester.integration.testSupport.config;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.StationIdPair;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.config.closures.StationClosuresListForTest;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceTestConfig;
import com.tramchester.integration.testSupport.nptg.NPTGDataSourceTestConfig;
import com.tramchester.integration.testSupport.postcodes.PostCodeDatasourceConfig;
import com.tramchester.integration.testSupport.rail.RailRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.TramStations;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.*;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceTestConfig remoteNaptanXMLConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    protected final RemoteDataSourceConfig remoteNPTGconfig;

    protected final RailRemoteDataSourceConfig railRemoteDataSource;

    private static final List<TramStations> closedStations = List.of(PiccadillyGardens);

    public static final StationClosures marketStreetApril2025 = new StationClosuresListForTest(closedStations,
        UpcomingDates.PiccGardensWorksummer2025,
        true, Collections.emptySet(), Collections.emptySet());

    public static final List<StationClosures> CurrentClosures = List.of(marketStreetApril2025);

    private static final TemporaryStationsWalkIds piccadillyToMarketStreet = new TemporaryStationsWalkConfigForTest(
            StationIdPair.of(Piccadilly.getId(), MarketStreet.getId()), UpcomingDates.PiccGardensWorksummer2025);

    public static final List<TemporaryStationsWalkIds> CurrentStationWalks = Collections.singletonList(piccadillyToMarketStreet);

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
