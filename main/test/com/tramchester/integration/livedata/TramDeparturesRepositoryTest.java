package com.tramchester.integration.livedata;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.LiveDataDueTramsTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@LiveDataDueTramsTest
public class TramDeparturesRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private TramDepartureRepository departuresRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled),
                TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();


    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }


    @BeforeEach
    void onceBeforeEachTest() {
        LiveDataFetcher liveDataFetcher = componentContainer.get(LiveDataFetcher.class);

        departuresRepository = componentContainer.get(TramDepartureRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);

        liveDataFetcher.fetch();

    }

    @Test
    void shouldHaveStationsWithData() {
        int haveDataFor = departuresRepository.getNumStationsWithData(TestEnv.LocalNow());
        assertTrue(haveDataFor>0, "no data found");

    }

    @Test
    void shouldHaveStationWithUpToDateEntries() {
        int areUpToDate = departuresRepository.upToDateEntries();
        assertTrue(areUpToDate>0, "no up to data data");
    }

    @Test
    void shouldHaveStationWithDueTrams() {

        Station station = null;
        List<UpcomingDeparture> found = Collections.emptyList();
        for(Station each : stationRepository.getStations()) {
            found = departuresRepository.forStation(each);
            if (!found.isEmpty()) {
                station = each;
                break;
            }
        }

        assertFalse(found.isEmpty(), "no departures");
        assertNotNull(station);

        UpcomingDeparture departure = found.get(0);
        assertEquals(station, departure.getDisplayLocation());
    }


}
