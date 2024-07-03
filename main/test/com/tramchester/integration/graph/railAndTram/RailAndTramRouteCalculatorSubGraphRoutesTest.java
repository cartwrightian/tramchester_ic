package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.Modes.RailOnly;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@GMTest
public class RailAndTramRouteCalculatorSubGraphRoutesTest {
    private static final Duration TXN_TIMEOUT = Duration.ofMinutes(5);
    private static StationRepository stationRepository;
    private static TramchesterConfig config;

    private static final TramDate when = TestEnv.testDay();

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private static final List<IdFor<Station>> stations = Stream.of(Victoria, ExchangeSquare, StPetersSquare,
            Deansgate, Cornbrook, Pomona, ExchangeQuay, SalfordQuay, Anchorage, HarbourCity,
            MediaCityUK, Broadway, Langworthy, Weaste, Ladywell, Eccles,
            RailStationIds.ManchesterVictoria,
            RailStationIds.ManchesterOxfordRoad).map(HasId::getId).toList();

    private MutableGraphTransaction txn;
    private RouteCalculatorTestFacade testFacade;
    private Duration maxDurationFromConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new Config();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RailAndTramRouteCalculatorSubGraphRoutesTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        stationRepository = componentContainer.get(StationRepository.class);
        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        stations.forEach(graphFilter::addStation);
        // need both agencies for realistic test
        graphFilter.addAgency(TrainOperatingCompanies.NT.getAgencyId());
        graphFilter.addAgency(MutableAgency.METL);
        // GM train stations
        transportData.getStations(RailOnly).forEach(station -> graphFilter.addStation(station.getId()));
    }

    @AfterEach
    void afterAllEachTestsHasRun() {
        txn.close();
    }

    @AfterAll
    static void afterAllTestsRun() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTxMutable(TXN_TIMEOUT);
        testFacade = new RouteCalculatorTestFacade(componentContainer, txn);

        maxDurationFromConfig = Duration.ofMinutes(config.getMaxJourneyDuration());
    }

    // Notes:
    // Met and NT agency - fail
    // Met and NT agency, specific tram stations - pass
    // Met Only - pass
    // No filtering - fail

    @Test
    void shouldHaveVictoriaToEccles() {
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        //journeyRequest.setDiag(true);

        List<Journey> journeys = testFacade.calculateRouteAsList(Victoria, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    private static class Config extends RailAndTramGreaterManchesterConfig {
        public Config() {
            super();
        }

        @Override
        public Path getCacheFolder() {
            return TestEnv.CACHE_DIR.resolve("RailAndTramRouteCalculatorSubGraphRoutesTest");
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }
    }

}
