package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.mappers.MatchLiveTramToJourneyDestination;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;


@GMTest
@Disabled("WIP and need to find way to make reliable, dependent on departures being at certain locations")
public class MatchLiveTramOrTrainToJourneyDestinationTest {

    private static GuiceContainerDependencies componentContainer;
    private MatchLiveTramToJourneyDestination matchToJourneyDest;
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
        matchToJourneyDest = componentContainer.get(MatchLiveTramToJourneyDestination.class);
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

        List<UpcomingDeparture> all = getAllDepartures(journeyStations, EnumSet.of(Tram));

        IdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());
        List<UpcomingDeparture> trams = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure, journeyDestinations, journeyDestination.getId())).toList();

        assertFalse(trams.isEmpty());
    }

    @Test
    void shouldManPiccToStockportTowardsEuston() {
        Station euston = RailStationIds.LondonEuston.from(crsRepository);

        Station journeyStart = RailStationIds.ManchesterPiccadilly.from(stationRepository);
        Station journeyDestination = RailStationIds.Stockport.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        IdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());

        List<UpcomingDeparture> all = getAllDepartures(journeyStations, EnumSet.of(Train));
        assertFalse(all.isEmpty());

        List<UpcomingDeparture> depsTowardsLondon = all.stream().filter(dep -> dep.getDestinationId().equals(euston.getId())).toList();
        assertFalse(depsTowardsLondon.isEmpty());

        List<UpcomingDeparture> matching = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure, journeyDestinations, journeyDestination.getId())).toList();

        assertFalse(matching.isEmpty());


        List<UpcomingDeparture> towardsLondon = matching.stream().filter(train -> train.getDestinationId().equals(euston.getId())).toList();

        assertFalse(towardsLondon.isEmpty());
    }

    @Test
    void shouldManPiccToStockportTowardsHazelGrove() {

        Station hazelGrove = stationRepository.getStationById(Station.createId("HAZL"));
        Station journeyStart = RailStationIds.ManchesterPiccadilly.from(stationRepository);
        Station journeyDestination = RailStationIds.Stockport.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        IdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());

        List<UpcomingDeparture> all = getAllDepartures(journeyStations, EnumSet.of(Train));
        assertFalse(all.isEmpty());

        List<UpcomingDeparture> matching = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure, journeyDestinations, journeyDestination.getId())).toList();

        assertFalse(matching.isEmpty());

        List<UpcomingDeparture> towardsHazelGrove = matching.stream().
                filter(train -> train.getDestinationId().equals(hazelGrove.getId())).toList();

        assertFalse(towardsHazelGrove.isEmpty());
    }


    private List<UpcomingDeparture> getAllDepartures(final StationPair journeyStations, EnumSet<TransportMode> modes) {
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
        return departuresRepository.getDueForLocation(journeyStations.getBegin(), now.toLocalDate(), time, modes);


    }


}
