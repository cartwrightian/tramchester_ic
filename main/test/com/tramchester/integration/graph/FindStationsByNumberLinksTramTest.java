package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.Interchanges;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.FakeStation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

// TODO Combine with FindLinkedStationsTest
class FindStationsByNumberLinksTramTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private FindLinkedStations finder;
    private int threshold;

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
        threshold = Interchanges.getLinkThreshhold(TransportMode.Tram);
        finder = componentContainer.get(FindLinkedStations.class);
    }

    /**
     * @see com.tramchester.testSupport.AdditionalTramInterchanges
     */
    @Test
    void shouldNotDuplicateWithConfig() {

        List<GTFSSourceConfig> dataSources = config.getGtfsSourceConfig();
        assertEquals(1, dataSources.size());

        GTFSSourceConfig dataSource = dataSources.getFirst();
        assertEquals(DataSourceID.tfgm, dataSource.getDataSourceId());
        ImmutableIdSet<Station> additionalInterchanges = dataSource.getAdditionalInterchanges();

        IdSet<Station> stationWithLinks = finder.atLeastNLinkedStations(TransportMode.Tram, threshold);

        assertTrue(stationWithLinks.containsNoneOf(additionalInterchanges), "\nFound also in config " + additionalInterchanges +
                " \nstations with links were " + stationWithLinks);
        assertTrue(additionalInterchanges.containsNoneOf(stationWithLinks), "\nFound also in config " + stationWithLinks +
                " \nstations with links were " + additionalInterchanges);
    }

    // WIP Spike - likely remove
    //@DisabledUntilDate(year = 2026, month = 10, day = 27)
    @Test
    void shouldServiceIdsForLinks() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        Station station = Cornbrook.from(stationRepository);

        // needs to have at least threshold links
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        assertTrue(interchangeRepository.isInterchange(station), "Not an interchange");

        List<IdSet<Service>> results = finder.serviceIdsPerLink(station);

        assertFalse(results.isEmpty());

        IdSet<Service> dropOffs = station.getDropoffRoutes().stream().
                flatMap(route -> route.getServices().stream()).
                collect(IdSet.collector());

        IdSet<Service> pickups = station.getPickupRoutes().stream().
                flatMap(route -> route.getServices().stream()).
                collect(IdSet.collector());

        IdSet<Service> allServices = IdSet.union(dropOffs, pickups);

        results.forEach(servicesPerLink -> {
            IdSet<Service> mismatch = servicesPerLink.removeAll(allServices);
            assertTrue(mismatch.isEmpty(), "Mismatch %s between expected %s and %s".
                    formatted(mismatch, allServices, servicesPerLink));
        });

    }


    @Test
    void shouldIdInterchangePointsLinked() {

        IdSet<Station> found = finder.atLeastNLinkedStations(TransportMode.Tram, threshold);

        IdSet<Station> expected = FakeStation.IdSetOf(
                PiccadillyGardens,
                MarketStreet,
                TraffordBar,
                Cornbrook,
                Victoria,
                StPetersSquare,
                // closures and timetable changes
                StWerburghsRoad,
                Pomona,
                Broadway,
                HarbourCity
                //Shudehill
                // Autumn 2026
                //Piccadilly, Firswood, MediaCityUK, Deansgate, Sale, Wharfside, Stretford
        );

        TestEnv.Autumn2026Connections.forEach(fakeStation -> expected.add(fakeStation.getId()));

        ImmutableIdSet<Station> diff = IdSet.disjunction(found, expected);

        assertTrue(diff.isEmpty(), "Different: " + diff + "\nbetween expected:\n" + expected + " \nfound:" + found);

    }

}
