package com.tramchester.unit.geo;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.filters.IncludeAllFilter;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static com.tramchester.testSupport.reference.KnownLocations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationLocationsFromTestDataTest extends EasyMockSupport {

    private static TramchesterConfig config;
    private static EnumSet<TransportMode> modes;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private StationLocations stationLocations;
    private StationGroupsRepository compositeStationRepository;

    @BeforeAll
    static void onceBeforeAllTestRuns() {
        config = TestEnv.GET();
        modes = config.getTransportModes();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        NaptanRepository naptanRepository = createMock(NaptanRepository.class);
        NPTGRepository nptgRepository = createMock(NPTGRepository.class);
        ProvidesNow providesNow = new ProvidesLocalNow();
        GraphFilter graphFilter = new IncludeAllFilter();

        EasyMock.expect(naptanRepository.isEnabled()).andStubReturn(true);

        TramTransportDataForTestFactory factory = new TramTransportDataForTestFactory(providesNow);
        factory.start();

        transportData = factory.getTestData();

        stationLocations = new StationLocations(transportData, transportData, naptanRepository, new Geography(config));
        compositeStationRepository = new StationGroupsRepository(transportData, config, nptgRepository, naptanRepository, graphFilter);
    }

    @Disabled("Need way to inject naptan test data here")
    @Test
    void shouldFindFirstStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearAltrincham.getGridPosition(), 3,
                MarginInMeters.ofMeters(1000), modes);
        assertEquals(1, results.size(), results.toString());

        // fallback name, no naptan area data loaded
        replayAll();
        compositeStationRepository.start();
        assertEquals(compositeStationRepository.findByName("Id{'area1'}"), results.get(0));
        verifyAll();
    }

    @Disabled("Need way to inject naptan test data here")
    @Test
    void shouldFindFourthStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearKnutsfordBusStation.location(), 3,
                MarginInMeters.ofMeters(1000), modes);
        replayAll();
        assertEquals(1, results.size(), results.toString());

        // fallback name, no naptan area data loaded
        assertEquals(compositeStationRepository.findByName("Id{'area4'}"), results.getFirst());
        verifyAll();
    }

    @Test
    void shouldFindSecondStation() {
        replayAll();
//        stationLocations.start();

        List<Station> results = stationLocations.nearestStationsSorted(nearWythenshaweHosp.location(), 3,
                MarginInMeters.ofMeters(500), modes);

        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getSecond()));
        verifyAll();
    }

    @Test
    void shouldFindLastStation() {
        replayAll();
        List<Station> results = stationLocations.nearestStationsSorted(nearPiccGardens.location(), 3,
                MarginInMeters.ofMeters(500), modes);
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getLast()));
        verifyAll();
    }

    @Test
    void shouldFindInterchange() {
        replayAll();
        List<Station> results = stationLocations.nearestStationsSorted(nearShudehill.location(), 3,
                MarginInMeters.ofMeters(500), modes);
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getInterchange()));
        verifyAll();
    }

    @Test
    void shouldFindNearStockport() {
        replayAll();
        List<Station> results = stationLocations.nearestStationsSorted(nearStockportBus.location(), 3,
                MarginInMeters.ofMeters(500), modes);
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getFifthStation()));
        verifyAll();
    }

}
