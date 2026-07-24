package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.mappers.MatchDeparturesToJourneyDestination;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.TrainOnly;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;


@GMTest
@Disabled("WIP and need to find way to make reliable, dependent on departures being at certain locations")
public class MatchLiveTramOrTrainToJourneyDestinationTest {

    private static GuiceContainerDependencies componentContainer;
    private MatchDeparturesToJourneyDestination matchToJourneyDest;
    private StationRepository stationRepository;
    private LiveDataMarshaller liveDataMarshaller;
    private LiveDataFetcher fetcher;
    private DeparturesRepository departuresRepository;
    private CRSRepository crsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        matchToJourneyDest = componentContainer.get(MatchDeparturesToJourneyDestination.class);
        liveDataMarshaller = componentContainer.get(LiveDataMarshaller.class);
        departuresRepository = componentContainer.get(DeparturesRepository.class);
        fetcher = componentContainer.get(LiveDataFetcher.class);
        crsRepository = componentContainer.get(CRSRepository.class);
    }

    @Test
    void shouldTramsCornBrookToStPetersSquare() {
        Station journeyStart = TramStations.Cornbrook.from(stationRepository);
        Station journeyDestination = TramStations.StPetersSquare.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        DestinationAndCallingPoints destinationAndCallingPoints = DestinationAndCallingPoints.None();
        List<UpcomingDeparture> all = getAllDepartures(journeyStations, TransportMode.TramsOnly, destinationAndCallingPoints);

        ImmutableIdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());
        List<UpcomingDeparture> trams = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure,
                        createDestAndCalling(journeyDestination.getId(), journeyDestinations))).toList();

        assertFalse(trams.isEmpty());
    }

    private DestinationAndCallingPoints createDestAndCalling(IdFor<Station> dest, ImmutableIdSet<Station> calling) {
        return new DestinationAndCallingPoints(dest,calling);
    }

    @Test
    void shouldManPiccToStockportTowardsEuston() {
        Station euston = RailStationIds.LondonEuston.from(crsRepository);

        Station journeyStart = RailStationIds.ManchesterPiccadilly.from(stationRepository);
        Station stockport = RailStationIds.Stockport.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, stockport);

        ImmutableIdSet<Station> journeyDestinations = IdSet.singleton(stockport.getId());

        DestinationAndCallingPoints destinationAndCallingPoints = new DestinationAndCallingPoints(stockport.getId(),IdSet.emptySet());

        List<UpcomingDeparture> all = getAllDepartures(journeyStations, TrainOnly, destinationAndCallingPoints);
        assertFalse(all.isEmpty());

        List<UpcomingDeparture> depsTowardsLondon = all.stream().
                filter(dep -> dep.getDestinationId().equals(euston.getId())).toList();

        assertFalse(depsTowardsLondon.isEmpty());

        List<UpcomingDeparture> matching = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure,
                        createDestAndCalling(stockport.getId(), journeyDestinations))).toList();

        assertFalse(matching.isEmpty());

        List<UpcomingDeparture> towardsLondon = matching.stream().
                filter(train -> train.getDestinationId().equals(euston.getId())).toList();

        assertFalse(towardsLondon.isEmpty());
    }

    @Test
    void shouldManPiccToStockportTowardsEustonMatchingDest() {
        fail("todo - add test for matching dest here");
    }

    @Test
    void shouldManPiccToStockportTowardsEustonMatchingCallingPoints() {
        fail("todo - add test for matching calling points here");
    }

    @Test
    void shouldManPiccToStockportTowardsHazelGrove() {

        Station hazelGrove = stationRepository.getStationById(Station.createId("HAZL"));
        Station journeyStart = RailStationIds.ManchesterPiccadilly.from(stationRepository);
        Station journeyDestination = RailStationIds.Stockport.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        ImmutableIdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());

        DestinationAndCallingPoints destinationAndCallingPoints = DestinationAndCallingPoints.None();
        List<UpcomingDeparture> all = getAllDepartures(journeyStations, TrainOnly, destinationAndCallingPoints);
        assertFalse(all.isEmpty());

        List<UpcomingDeparture> matching = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure,
                        createDestAndCalling(journeyDestination.getId(), journeyDestinations))).toList();

        assertFalse(matching.isEmpty());

        List<UpcomingDeparture> towardsHazelGrove = matching.stream().
                filter(train -> train.getDestinationId().equals(hazelGrove.getId())).toList();

        assertFalse(towardsHazelGrove.isEmpty());
    }


    private List<UpcomingDeparture> getAllDepartures(final StationPair journeyStations, final ImmutableEnumSet<TransportMode> modes,
                                                     DestinationAndCallingPoints destinationAndCallingPoints) {
        final CountDownLatch latch = new CountDownLatch(1);

        // need to wait until we have some live data
        liveDataMarshaller.addSubscriber(update -> {
            latch.countDown();
            return true;
        });

        try {
            fetcher.fetch();
            latch.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e);
        }

        //IdSet<Station> journeyDestinations = IdSet.singleton(journeyStations.getEnd().getId());

        final LocalDateTime now = TestEnv.LocalNow();
        final TramTime time = TramTime.ofHourMins(now.toLocalTime());
        return departuresRepository.getDueForLocation(journeyStations.getBegin(), now.toLocalDate(), time, modes,
                destinationAndCallingPoints);


    }


}
