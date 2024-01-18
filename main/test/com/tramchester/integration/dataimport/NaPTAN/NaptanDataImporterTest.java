package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataCallbackImporter;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.reference.BusStations.BuryInterchange;
import static org.junit.jupiter.api.Assertions.*;

class NaptanDataImporterTest {

    private static GuiceContainerDependencies componentContainer;
    private static List<NaptanStopData> loadedStops;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan(
                EnumSet.of(TransportMode.Bus, TransportMode.Tram, TransportMode.Train));

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        NaptanDataCallbackImporter dataImporter = componentContainer.get(NaptanDataCallbackImporter.class);

        loadedStops = new ArrayList<>();
        dataImporter.loadData(element -> loadedStops.add(element));
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        loadedStops.clear();
        componentContainer.close();
    }

    // was for initial diagnostics, likely changes too often
    @Test
    void shouldHaveLoadedSomeData() {
        assertTrue(loadedStops.size() > 400000, "m");
    }

    @Test
    void shouldLoadKnownBusStation() {

        IdFor<NaptanRecord> buryId = StringIdFor.convert(BuryInterchange.getId(), NaptanRecord.class);

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode().isValid()).
                filter(stop -> stop.getAtcoCode().equals(buryId)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
    }

    @Test
    void shouldLoadKnownTramStation() {

        IdFor<NaptanRecord> id = StringIdFor.convert(TramStations.StPetersSquare.getId(), NaptanRecord.class);

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode().isValid()).
                filter(stop -> stop.getAtcoCode().equals(id)).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopData known = foundKnown.get();
        assertEquals(NaptanStopType.tramMetroUndergroundAccess, known.getStopType());
    }

    @Test
    void shouldContainOutofAreaStop() {
        IdFor<NaptanRecord> id = NaptanRecord.createId(TestEnv.BRISTOL_BUSSTOP_OCTOCODE);

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode().isValid()).
                filter(stop -> stop.getAtcoCode().equals(id)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
        NaptanStopData known = foundKnown.get();
        assertEquals(NaptanStopType.busCoachTrolleyStopOnStreet, known.getStopType());
    }

}
