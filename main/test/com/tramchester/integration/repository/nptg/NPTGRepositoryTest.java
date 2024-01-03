package com.tramchester.integration.repository.nptg;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.nptg.NPTGRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
@TrainTest
public class NPTGRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private NPTGRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan();
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

        IdFor<NaptanRecord> actoCode = NaptanRecord.createId("1800SJ11291");

        assertTrue(repository.hasActoCode(actoCode));
        NPTGData result = repository.getByActoCode(actoCode);

        assertEquals("Ashley Heath", result.getLocalityName());
        assertEquals("Altrincham", result.getParentLocalityName(), result.toString());
    }

    @Test
    void shouldHaveKnutsfordStop() {
        final IdFor<Station> actoCode = BusStations.KnutsfordStationStand3.getId();

        IdFor<NaptanRecord> id = StringIdFor.convert(actoCode, NaptanRecord.class);
        NPTGData result = repository.getByActoCode(id);
        assertEquals("Knutsford", result.getLocalityName());
    }

}
