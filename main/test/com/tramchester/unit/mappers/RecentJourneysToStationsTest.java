package com.tramchester.unit.mappers;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.mappers.RecentJourneysToStations;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.Stockport;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecentJourneysToStationsTest extends EasyMockSupport {

    private RecentJourneysToStations mapper;
    private StationRepository stationRepository;
    private MutableStation stockportRail;

    @BeforeEach
    void onceBeforeEachTestRuns() {

        stationRepository = createMock(StationRepository.class);
        mapper = new RecentJourneysToStations(stationRepository);

        stockportRail = new MutableStation(Stockport.getId(), NaptanArea.invalidId(), "Stockport Train",
                LatLong.Invalid, GridPosition.Invalid, DataSourceID.tfgm, "XXX64");
        Route railRoute = new MutableRoute(Route.createId("fake"), "short", "long",
                TestEnv.NorthernTrainsAgency(), TransportMode.Train);
        stockportRail.addRoutePickUp(railRoute);
    }

    @Test
    void shouldMapRecentJourneysToStationsAllValid() {
        RecentJourneys recentJourneys = new RecentJourneys();
        LocalDateTime timeStamp = TestEnv.LocalNow();

        List<IdFor<Station>> stationIds = List.of(Altrincham.getId(), Bury.getId());

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp)).collect(Collectors.toSet());
        recentJourneys.setRecentIds(timestampedIds);

        EasyMock.expect(stationRepository.hasStationId(Altrincham.getId())).andReturn(true);
        EasyMock.expect(stationRepository.getStationById(Altrincham.getId())).andReturn(Altrincham.fake());
        EasyMock.expect(stationRepository.hasStationId(Bury.getId())).andReturn(true);
        EasyMock.expect(stationRepository.getStationById(Bury.getId())).andReturn(Bury.fake());

        replayAll();
        Set<Station> stations = mapper.from(recentJourneys, EnumSet.allOf(TransportMode.class));
        verifyAll();

        assertEquals(timestampedIds.size(), stations.size());
    }

    @Test
    void shouldMapRecentJourneysToStationsMissingId() {
        RecentJourneys recentJourneys = new RecentJourneys();
        LocalDateTime timeStamp = TestEnv.LocalNow();

        List<IdFor<Station>> stationIds = List.of(Altrincham.getId(), Bury.getId());

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp)).collect(Collectors.toSet());
        recentJourneys.setRecentIds(timestampedIds);

        EasyMock.expect(stationRepository.hasStationId(Altrincham.getId())).andReturn(true);
        EasyMock.expect(stationRepository.getStationById(Altrincham.getId())).andReturn(Altrincham.fake());
        EasyMock.expect(stationRepository.hasStationId(Bury.getId())).andReturn(false);

        replayAll();
        Set<Station> stations = mapper.from(recentJourneys, EnumSet.allOf(TransportMode.class));
        verifyAll();

        assertEquals(1, stations.size());
        assertTrue(stations.contains(Altrincham.fake()));
    }

    @Test
    void shouldMapRecentJourneysToStationsAllMixedMode() {
        RecentJourneys recentJourneys = new RecentJourneys();
        LocalDateTime timeStamp = TestEnv.LocalNow();

        List<IdFor<Station>> stationIds = List.of(Altrincham.getId(), Stockport.getId());

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp)).collect(Collectors.toSet());
        recentJourneys.setRecentIds(timestampedIds);

        EasyMock.expect(stationRepository.hasStationId(Altrincham.getId())).andReturn(true);
        EasyMock.expect(stationRepository.getStationById(Altrincham.getId())).andReturn(Altrincham.fake());
        EasyMock.expect(stationRepository.hasStationId(Stockport.getId())).andReturn(true);
        EasyMock.expect(stationRepository.getStationById(Stockport.getId())).andReturn(stockportRail);

        replayAll();
        Set<Station> all = mapper.from(recentJourneys, EnumSet.allOf(TransportMode.class));
        verifyAll();

        assertEquals(2, all.size());
        assertTrue(all.contains(Altrincham.fake()));
        assertTrue(all.contains(stockportRail));
    }

    @Test
    void shouldMapRecentJourneysToStationsFilterByMode() {
        RecentJourneys recentJourneys = new RecentJourneys();
        LocalDateTime timeStamp = TestEnv.LocalNow();

        List<IdFor<Station>> stationIds = List.of(Altrincham.getId(), Bury.getId(), Stockport.getId());

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp)).collect(Collectors.toSet());
        recentJourneys.setRecentIds(timestampedIds);

        EasyMock.expect(stationRepository.hasStationId(Altrincham.getId())).andStubReturn(true);
        EasyMock.expect(stationRepository.getStationById(Altrincham.getId())).andStubReturn(Altrincham.fake());
        EasyMock.expect(stationRepository.hasStationId(Bury.getId())).andStubReturn(true);
        EasyMock.expect(stationRepository.getStationById(Bury.getId())).andStubReturn(Bury.fake());
        EasyMock.expect(stationRepository.hasStationId(Stockport.getId())).andStubReturn(true);
        EasyMock.expect(stationRepository.getStationById(Stockport.getId())).andStubReturn(stockportRail);

        replayAll();
        Set<Station> tramStations = mapper.from(recentJourneys, EnumSet.of(TransportMode.Tram));
        Set<Station> trainStations = mapper.from(recentJourneys, EnumSet.of(TransportMode.Train));
        verifyAll();

        assertEquals(2, tramStations.size());
        assertTrue(tramStations.contains(Altrincham.fake()));
        assertTrue(tramStations.contains(Bury.fake()));

        assertEquals(1, trainStations.size());
        assertTrue(trainStations.contains(stockportRail));
    }
}
