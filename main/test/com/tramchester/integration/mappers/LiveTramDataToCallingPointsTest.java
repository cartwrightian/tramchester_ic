package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.mappers.LiveTramDataToCallingPoints;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;


@Disabled("WIP")
public class LiveTramDataToCallingPointsTest {

    private static GuiceContainerDependencies componentContainer;
    private LiveTramDataToCallingPoints toCallingPoints;
    private StationRepository stationRepository;
    private LiveDataMarshaller liveDataMarshaller;
    private LiveDataFetcher fetcher;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        toCallingPoints = componentContainer.get(LiveTramDataToCallingPoints.class);
        liveDataMarshaller = componentContainer.get(LiveDataMarshaller.class);
        fetcher = componentContainer.get(LiveDataFetcher.class);
    }

    @Test
    void shouldTramsCornBrookToStPetersSquare() {
        Station journeyStart = TramStations.Cornbrook.from(stationRepository);
        Station journeyDestination = TramStations.StPetersSquare.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        List<UpcomingDeparture> trams = getUpcomingDepartures(journeyStations);

        assertFalse(trams.isEmpty());
    }

    @Test
    void shouldHaveTramsGoingFromCornbrookToAltrincham() {

        Station journeyStart = TramStations.Cornbrook.from(stationRepository);
        Station journeyDestination = TramStations.Altrincham.from(stationRepository);

        StationPair journeyStations = StationPair.of(journeyStart, journeyDestination);

        List<UpcomingDeparture> trams = getUpcomingDepartures(journeyStations);

        assertFalse(trams.isEmpty());

        trams.forEach(tram -> {
            assertEquals(TramStations.Altrincham.getId(), tram.getDestination().getId(), "departure was " + tram);
        });

    }

    private List<UpcomingDeparture> getUpcomingDepartures(StationPair journeyStations) {
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

        LocalDateTime localDateTime = TestEnv.LocalNow();
        TramTime currentTime = TramTime.of(localDateTime.getHour(), localDateTime.getMinute());

        List<UpcomingDeparture> trams = toCallingPoints.nextTramFor(journeyStations, localDateTime.toLocalDate(), currentTime, EnumSet.of(Tram));
        return trams;
    }


}
