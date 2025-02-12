package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.Interchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.FakeStation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindStationsByNumberLinksTramTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private FindStationsByNumberLinks finder;
    private int threshhold;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        threshhold = Interchanges.getLinkThreshhold(TransportMode.Tram);
        finder = componentContainer.get(FindStationsByNumberLinks.class);
    }

    /**
     * @see com.tramchester.testSupport.AdditionalTramInterchanges
     */
    @Test
    void shouldNotDuplicateWithConfig() {

        List<GTFSSourceConfig> dataSources = config.getGTFSDataSource();
        assertEquals(1, dataSources.size());

        GTFSSourceConfig dataSource = dataSources.getFirst();
        assertEquals(DataSourceID.tfgm, dataSource.getDataSourceId());
        IdSet<Station> additionalInterchanges = dataSource.getAdditionalInterchanges();

        IdSet<Station> stationWithLinks = finder.atLeastNLinkedStations(TransportMode.Tram, threshhold);

        IdSet<Station> inConfigAndStationsWithLinks = IdSet.intersection(stationWithLinks, additionalInterchanges);

        assertTrue(inConfigAndStationsWithLinks.isEmpty(), "\nFound also in config " + inConfigAndStationsWithLinks +
                " \nstations with links were " + stationWithLinks);
    }

    @Test
    void shouldIdInterchangePointsLinked() {

        IdSet<Station> found = finder.atLeastNLinkedStations(TransportMode.Tram, threshhold);

        List<IdFor<Station>> expectedList = Stream.of(
                Shudehill,
                StPetersSquare,
                PiccadillyGardens,
                //Piccadilly,
                MarketStreet,
                TraffordBar,
                Cornbrook,
                Victoria,
                StWerburghsRoad,
                Pomona,
                Broadway,
                HarbourCity
            ).map(FakeStation::getId).toList();

        IdSet<Station> expected = new IdSet<>(expectedList);
        IdSet<Station> diff = IdSet.disjunction(found, expected);

        assertTrue(diff.isEmpty(), diff + " between expected:\n" + expected + " \nfound:" + found);

    }

}
