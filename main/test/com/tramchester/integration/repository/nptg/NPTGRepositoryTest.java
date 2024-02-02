package com.tramchester.integration.repository.nptg;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@GMTest
@TrainTest
public class NPTGRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private NPTGRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan(
                EnumSet.of(TransportMode.Bus, TransportMode.Tram, TransportMode.Train));
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = componentContainer.get(NPTGRepository.class);
    }

    @Test
    void shouldGetKnownLocationData() {

        IdFor<NPTGLocality> id = NPTGLocality.createId("N0074933");

        assertTrue(repository.hasLocaility(id));

        NPTGLocality result = repository.get(id);
        assertEquals("Castlefield", result.getLocalityName());
        assertEquals("Manchester City Centre", result.getParentLocalityName(), result.toString());
    }

    @Test
    void shouldGetLocalityOnEdgeOfGMBounds() {
        IdFor<NPTGLocality> id = NPTGLocality.createId("E0044412");

        assertTrue(repository.hasLocaility(id));
        NPTGLocality result = repository.get(id);
        assertEquals("Cuddington", result.getLocalityName());
    }

    @Test
    void shouldNotHaveWestminsterAsOutOfBounds() {

        IdFor<NPTGLocality> id = KnownLocality.LondonWestminster.getAreaId();

        assertFalse(repository.hasLocaility(id));
    }

    @Test
    void shouldHaveRecordForAllLoadedNaptanStops() {
        // to assist in setting margin for bounds
        NaptanRepository naptanRepository = componentContainer.get(NaptanRepository.class);

        IdSet<NPTGLocality> missingRecords = naptanRepository.getAll().
                map(NaptanRecord::getLocalityId).
                filter(localityId -> !repository.hasLocaility(localityId)).
                collect(IdSet.idCollector());

        assertEquals(0, missingRecords.size(), missingRecords.toString());
    }


}
