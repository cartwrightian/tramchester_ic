package com.tramchester.integration.livedata;

import com.google.common.collect.Sets;
import com.thalesgroup.rtti._2017_10_01.ldb.types.*;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.livedata.openLdb.TrainDeparturesDataFetcher;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.TrainLiveDataTest;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TrainLiveDataTest
@GMTest
@TrainTest
class TrainDeparturesDataFetcherTest {
    private static GuiceContainerDependencies componentContainer;
    private TrainDeparturesDataFetcher dataFetcher;
    private StationRepository stationRepository;
    private CRSRepository crsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new RailAndTramGreaterManchesterConfig(),
                TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        dataFetcher = componentContainer.get(TrainDeparturesDataFetcher.class);
        stationRepository = componentContainer.get(StationRepository.class);
        crsRepository = componentContainer.get(CRSRepository.class);
    }

    @Test
    void testShouldGetDeparturesForStation() {
        Optional<StationBoard> maybeBoard = dataFetcher.getFor(RailStationIds.ManchesterPiccadilly.from(stationRepository));

        assertTrue(maybeBoard.isPresent(), "no station board returned");

        StationBoard board = maybeBoard.get();

        assertEquals("MAN", board.getCrs());

        List<ServiceItem> services = board.getTrainServices().getService();

        assertFalse(services.isEmpty());

    }

    @Test
    void testShouldGetDeparturesForStationDestAndCallingPoints() {

        Station from = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        ImmutableIdSet<Station> callingPoints = Stream.of(RailStationIds.Stockport, RailStationIds.StokeOnTrent).
                map(FakeStation::getId).collect(IdSet.idCollector());

        IdFor<Station> destId = RailStationIds.LondonEuston.getId();

        DestinationAndCallingPoints destAndCalling = new DestinationAndCallingPoints(destId,
                callingPoints);

        Optional<DeparturesBoardWithDetails> maybeBoard = dataFetcher.getFor(from, destAndCalling);

        assertTrue(maybeBoard.isPresent(), "no station board returned");

        DeparturesBoardWithDetails board = maybeBoard.get();

        assertEquals("MAN", board.getCrs());

        ArrayOfDepartureItemsWithCallingPoints departures = board.getDepartures();
        List<DepartureItemWithCallingPoints> dests = departures.getDestination().stream().
                filter(item -> item.getService()!=null).toList();

        assertFalse(dests.isEmpty(), "not dests for " + departures);

        dests.forEach(dest -> {
            // could be a range of destinations
            //assertEquals("EUS", dest.getCrs());

            ServiceItemWithCallingPoints service = dest.getService();
            //assertTrue(match(destId, service.getCurrentDestinations()));
            assertTrue(match(callingPoints, service.getSubsequentCallingPoints()));
        });

    }

    @Test
    void testShouldGetDeparturesForStationDestAndTramEndpoint() {

        Station from = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        ImmutableIdSet<Station> callingPoints = Stream.of(RailStationIds.Stockport).
                map(FakeStation::getId).collect(IdSet.idCollector());

        IdFor<Station> destId = TramStations.Altrincham.getId();

        DestinationAndCallingPoints destAndCalling = new DestinationAndCallingPoints(destId,
                callingPoints);

        Optional<DeparturesBoardWithDetails> maybeBoard = dataFetcher.getFor(from, destAndCalling);

        assertTrue(maybeBoard.isPresent(), "no station board returned");

        DeparturesBoardWithDetails board = maybeBoard.get();

        assertEquals("MAN", board.getCrs());

        ArrayOfDepartureItemsWithCallingPoints departures = board.getDepartures();
        List<DepartureItemWithCallingPoints> dests = departures.getDestination().stream().
                filter(item -> item.getService()!=null).toList();

        assertFalse(dests.isEmpty(), "no dests for " + departures);

        dests.forEach(dest -> {
            // could be a range of destinations
            //assertEquals("EUS", dest.getCrs());

            ServiceItemWithCallingPoints service = dest.getService();
            //assertTrue(match(destId, service.getCurrentDestinations()));
            assertTrue(match(callingPoints, service.getSubsequentCallingPoints()));
        });

    }

    private boolean match(ImmutableIdSet<Station> callingPoints, ArrayOfArrayOfCallingPoints subsequentCallingPoints) {
        Set<String> crs = subsequentCallingPoints.getCallingPointList().stream().
                flatMap(list -> list.getCallingPoint().stream()).
                map(CallingPoint::getCrs).
                collect(Collectors.toSet());
        Set<String> expectecCRS = callingPoints.stream().map(id -> crsRepository.getCRSCodeFor(id)).collect(Collectors.toSet());

        Sets.SetView<String> union = Sets.intersection(crs, expectecCRS);
        return !union.isEmpty();

    }

//    private boolean match(IdFor<Station> destId, ArrayOfServiceLocations currentDestinations) {
//        Set<String> crs = currentDestinations.getLocation().stream().
//                map(ServiceLocation::getCrs).
//                collect(Collectors.toSet());
//        String destCRS = crsRepository.getCRSCodeFor(destId);
//        return crs.contains(destCRS);
//
//    }

}
