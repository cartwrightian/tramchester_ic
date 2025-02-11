package com.tramchester.unit.mappers;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.mappers.RecentJourneysToLocations;
import com.tramchester.repository.LocationRepository;
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

public class RecentJourneysToLocationsTest extends EasyMockSupport {

    private final IdForDTO altrinchamIdForDTO = Altrincham.getIdForDTO();
    private final IdForDTO buryIdForDTO = Bury.getIdForDTO();
    private final IdForDTO stockportIdDTO = Stockport.getIdForDTO();

    private RecentJourneysToLocations mapper;
    private LocationRepository locationRepository;
    private MutableStation stockportRail;


    // NOTE: expectLastCall here due to Capture of Location<?>

    @BeforeEach
    void onceBeforeEachTestRuns() {

        locationRepository = createMock(LocationRepository.class);
        mapper = new RecentJourneysToLocations(locationRepository);

        stockportRail = new MutableStation(Stockport.getId(), NPTGLocality.InvalidId(), "Stockport Train",
                LatLong.Invalid, GridPosition.Invalid, DataSourceID.tfgm, false);
        Route railRoute = new MutableRoute(Route.createId("fake"), "short", "long",
                TestEnv.NorthernTrainsAgency(), TransportMode.Train);
        stockportRail.addRoutePickUp(railRoute);
    }

    @Test
    void shouldMapRecentJourneysToStationsAllValid() {
        RecentJourneys recentJourneys = new RecentJourneys();
        LocalDateTime timeStamp = TestEnv.LocalNow();

        List<IdFor<Station>> stationIds = List.of(Altrincham.getId(), Bury.getId());

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp, LocationType.Station)).collect(Collectors.toSet());
        recentJourneys.setTimeStamps(timestampedIds);

        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, altrinchamIdForDTO)).andReturn(true);

        EasyMock.expect(locationRepository.getLocation(LocationType.Station, altrinchamIdForDTO));
        EasyMock.expectLastCall().andReturn(Altrincham.fake());
        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, buryIdForDTO)).andReturn(true);
        EasyMock.expect(locationRepository.getLocation(LocationType.Station, buryIdForDTO));
        EasyMock.expectLastCall().andReturn(Bury.fake());

        replayAll();
        Set<Location<?>> locations = mapper.from(recentJourneys, EnumSet.allOf(TransportMode.class));
        verifyAll();

        assertEquals(timestampedIds.size(), locations.size());
    }

    @Test
    void shouldMapRecentJourneysToStationsMissingId() {
        RecentJourneys recentJourneys = new RecentJourneys();
        LocalDateTime timeStamp = TestEnv.LocalNow();

        List<IdFor<Station>> stationIds = List.of(Altrincham.getId(), Bury.getId());

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp, LocationType.Station)).collect(Collectors.toSet());
        recentJourneys.setTimeStamps(timestampedIds);

        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, altrinchamIdForDTO)).andReturn(true);
        EasyMock.expect(locationRepository.getLocation(LocationType.Station, altrinchamIdForDTO));
        EasyMock.expectLastCall().andReturn(Altrincham.fake());
        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, buryIdForDTO)).andReturn(false);

        replayAll();
        Set<Location<?>> locations = mapper.from(recentJourneys, EnumSet.allOf(TransportMode.class));
        verifyAll();

        assertEquals(1, locations.size());
        assertTrue(locations.contains(Altrincham.fake()));
    }

    @Test
    void shouldMapRecentJourneysToStationsAllMixedMode() {
        RecentJourneys recentJourneys = new RecentJourneys();
        LocalDateTime timeStamp = TestEnv.LocalNow();

        List<IdFor<Station>> stationIds = List.of(Altrincham.getId(), Stockport.getId());

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp, LocationType.Station)).collect(Collectors.toSet());
        recentJourneys.setTimeStamps(timestampedIds);

        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, altrinchamIdForDTO)).andReturn(true);
        EasyMock.expect(locationRepository.getLocation(LocationType.Station, altrinchamIdForDTO));
        EasyMock.expectLastCall().andReturn(Altrincham.fake());
        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, stockportIdDTO)).andReturn(true);
        EasyMock.expect(locationRepository.getLocation(LocationType.Station, stockportIdDTO));
        EasyMock.expectLastCall().andReturn(stockportRail);

        replayAll();
        Set<Location<?>> all = mapper.from(recentJourneys, EnumSet.allOf(TransportMode.class));
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

        Set<Timestamped> timestampedIds = stationIds.stream().map(id -> new Timestamped(id, timeStamp, LocationType.Station)).collect(Collectors.toSet());
        recentJourneys.setTimeStamps(timestampedIds);

        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, altrinchamIdForDTO)).andStubReturn(true);
        EasyMock.expect(locationRepository.getLocation(LocationType.Station, altrinchamIdForDTO));
        EasyMock.expectLastCall().andStubReturn(Altrincham.fake());
        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, buryIdForDTO)).andStubReturn(true);
        EasyMock.expect(locationRepository.getLocation(LocationType.Station, buryIdForDTO));
        EasyMock.expectLastCall().andStubReturn(Bury.fake());
        EasyMock.expect(locationRepository.hasLocation(LocationType.Station, stockportIdDTO)).andStubReturn(true);
        EasyMock.expect(locationRepository.getLocation(LocationType.Station, stockportIdDTO));
        EasyMock.expectLastCall().andStubReturn(stockportRail);

        replayAll();
        Set<Location<?>> tramStations = mapper.from(recentJourneys, EnumSet.of(TransportMode.Tram));
        Set<Location<?>> trainStations = mapper.from(recentJourneys, EnumSet.of(TransportMode.Train));
        verifyAll();

        assertEquals(2, tramStations.size());
        assertTrue(tramStations.contains(Altrincham.fake()));
        assertTrue(tramStations.contains(Bury.fake()));

        assertEquals(1, trainStations.size());
        assertTrue(trainStations.contains(stockportRail));
    }
}
