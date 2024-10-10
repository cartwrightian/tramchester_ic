package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.mappers.MatchLiveTramToJourneyDestination;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;


@Disabled("WIP and need to find way to make reliable, dependent on departures being at certain locations")
public class MatchLiveTramToJourneyDestinationTest {

    private static GuiceContainerDependencies componentContainer;
    private MatchLiveTramToJourneyDestination matchToJourneyDest;
    private StationRepository stationRepository;
    private LiveDataMarshaller liveDataMarshaller;
    private LiveDataFetcher fetcher;
    private DeparturesRepository departuresRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);
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
    }

    @Test
    void shouldTramsCornBrookToStPetersSquare() {
        Station journeyStart = TramStations.Cornbrook.from(stationRepository);
        Station journeyDestination = TramStations.StPetersSquare.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        List<UpcomingDeparture> all = getAllDepartures(journeyStations);

        IdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());
        List<UpcomingDeparture> trams = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure, journeyDestinations)).toList();

        assertFalse(trams.isEmpty());
    }

    @Test
    void shouldHaveTramsGoingFromCityCentreToAltrincham() {

        Station journeyStart = TramStations.Shudehill.from(stationRepository);
        Station journeyDestination = TramStations.Altrincham.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        List<UpcomingDeparture> all = getAllDepartures(journeyStations);

        IdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());

        assertFalse(all.isEmpty());

        List<UpcomingDeparture> matched = new ArrayList<>();
        all.forEach(tram -> {
            if (matchToJourneyDest.matchesJourneyDestination(tram, journeyDestinations)) {
                matched.add(tram);
                assertEquals(TramStations.Altrincham.getId(), tram.getDestination().getId(), "departure was " + tram);
            }
        });
        assertFalse(matched.isEmpty());

    }

    @Test
    void shouldHaveTramsGoingFromCornbrookToAltrincham() {

        Station journeyStart = TramStations.Cornbrook.from(stationRepository);
        Station journeyDestination = TramStations.Altrincham.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        List<UpcomingDeparture> all = getAllDepartures(journeyStations);

        IdSet<Station> journeyDestinations = IdSet.singleton(journeyDestination.getId());
        List<UpcomingDeparture> trams = all.stream().
                filter(departure -> matchToJourneyDest.matchesJourneyDestination(departure, journeyDestinations)).toList();

        assertFalse(trams.isEmpty());

        trams.forEach(tram -> {
            assertEquals(TramStations.Altrincham.getId(), tram.getDestination().getId(), "departure was " + tram);
        });

    }

    private List<UpcomingDeparture> getAllDepartures(final StationPair journeyStations) {
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

        IdSet<Station> journeyDestinations = IdSet.singleton(journeyStations.getEnd().getId());

        LocalDateTime now = TestEnv.LocalNow();
        TramTime time = TramTime.ofHourMins(now.toLocalTime());
        return departuresRepository.getDueForLocation(journeyStations.getBegin(), now.toLocalDate(), time, EnumSet.of(Tram));


    }


}
