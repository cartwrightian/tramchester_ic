package com.tramchester.integration.livedata;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlatformMessageRepositoryTest {
    private static ComponentContainer componentContainer;

    private PlatformMessageRepository messageRepo;

    public static final TramStations StationWithNotes = TramStations.StPetersSquare;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled),
                TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        // don't want to fetch every time
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        LiveDataFetcher liveDataFetcher = componentContainer.get(LiveDataFetcher.class);

        messageRepo = componentContainer.get(PlatformMessageRepository.class);

        liveDataFetcher.fetch();
    }

    @Test
    @LiveDataMessagesCategory
    void findAtleastOneStationWithNotes() {
        assertNotEquals(messageRepo.numberOfEntries(), 0);

        int numStationsWithMessages = messageRepo.numberStationsWithMessages(TestEnv.LocalNow());

        assertTrue(numStationsWithMessages>1);
    }

    @Test
    @LiveDataMessagesCategory
    void shouldHaveMessagesForTestStation() {
        Set<Station> stations = messageRepo.getStationsWithMessages(TestEnv.LocalNow().toLocalTime());

        assertTrue(stations.contains(StationWithNotes.fake()), "No message for " + StationWithNotes.getName()
                + " present for " + HasId.asIds(stations));
    }
}
