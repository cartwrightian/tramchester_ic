package com.tramchester.unit.graph.search;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.MapStatesToStages;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapStatesToStagesTest extends EasyMockSupport {

    private TramDate when;
    private StationRepository stationRepository;
    private PlatformRepository platformRepository;
    private TripRepository tripRepository;
    private TramTime queryTime;
    private MapStatesToStages mapper;
    private IdFor<Trip> tripId;
    private Trip trip;
    private Station navigationRoad;
    private IdFor<Platform> platformId;

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = createMock(StationRepository.class);
        platformRepository = createMock(PlatformRepository.class);
        tripRepository = createMock(TripRepository.class);

        queryTime = TramTime.of(10,0);
        when = TestEnv.testDay();

        mapper = new MapStatesToStages(stationRepository, platformRepository, tripRepository, queryTime);

        tripId = Trip.createId("tripAId");

        Service service = new MutableService(Service.createId("svcA"), DataSourceID.tfgm);
        trip = new MutableTrip(tripId, "tripHeadsign", service, TestEnv.getTramTestRoute(), Tram);

        EasyMock.expect(tripRepository.getTripById(tripId)).andStubReturn(trip);

        navigationRoad = NavigationRoad.fakeWithPlatform(1, when);
        EasyMock.expect(stationRepository.getStationById(navigationRoad.getId())).andStubReturn(navigationRoad);

        List<Platform> platforms = navigationRoad.getPlatforms().stream().toList();
        Platform platform = platforms.getFirst();
        platformId = platform.getId();

        EasyMock.expect(platformRepository.hasPlatformId(platformId)).andStubReturn(true);
        EasyMock.expect(platformRepository.getPlatformById(platformId)).andStubReturn(platform);

    }

    @Test
    void shouldTestVehicleStage() {

        Station altrincham = Altrincham.fake();
        EasyMock.expect(stationRepository.getStationById(altrincham.getId())).andStubReturn(altrincham);

        GraphNode boardNode = createMock(GraphNode.class);
        EasyMock.expect(boardNode.getStationId()).andStubReturn(navigationRoad.getId());
        EasyMock.expect(boardNode.getPlatformId()).andStubReturn(platformId);

        GraphNode leaveNode = createMock(GraphNode.class);
        EasyMock.expect(leaveNode.getStationId()).andStubReturn(altrincham.getId());

        replayAll();
        mapper.board(Tram, boardNode, true);
        mapper.recordTimeAtMinuteNode(TramTime.of(10,5), TramDuration.ofMinutes(5));
        mapper.beginTrip(tripId);
        mapper.recordTimeAtMinuteNode(TramTime.of(10,15), TramDuration.ofMinutes(15));
        mapper.leave(Tram, TramDuration.ofMinutes(23), leaveNode);
        mapper.atDestination(TramDuration.getInvalid());
        List<TransportStage<?, ?>> stages = mapper.getStages();
        verifyAll();

        assertEquals(1, stages.size());

        TransportStage<?, ?> stage = stages.getFirst();
        assertEquals(stage.getFirstStation().getId(), navigationRoad.getId());
        assertEquals(stage.getLastStation().getId(), altrincham.getId());
        assertEquals(stage.getTripId(), tripId);
        assertEquals(stage.getFirstDepartureTime(), TramTime.of(10,5));
        assertEquals(stage.getExpectedArrivalTime(), TramTime.of(10,5).plus(TramDuration.ofMinutes(23)));

    }

    @Test
    void shouldTestVehicleStageThenWalk() {

        Station altrincham = Altrincham.fake();
        EasyMock.expect(stationRepository.getStationById(altrincham.getId())).andStubReturn(altrincham);

        MyLocation walkEnd = MyLocation.create(KnownLocations.betweenAltrinchamAndNavigationRoad.latLong());

        GraphNode boardNode = createMock(GraphNode.class);
        EasyMock.expect(boardNode.getStationId()).andStubReturn(navigationRoad.getId());
        EasyMock.expect(boardNode.getPlatformId()).andStubReturn(platformId);

        GraphNode leaveNode = createMock(GraphNode.class);
        EasyMock.expect(leaveNode.getStationId()).andStubReturn(altrincham.getId());
        EasyMock.expect(leaveNode.hasLabel(GraphLabel.STATION)).andReturn(true);

        GraphNode walkEndNode = createMock(GraphNode.class);
        EasyMock.expect(walkEndNode.hasLabel(GraphLabel.STATION)).andReturn(false);
        EasyMock.expect(walkEndNode.getLatLong()).andStubReturn(walkEnd.getLatLong());

        replayAll();
        mapper.board(Tram, boardNode, true);
        mapper.recordTimeAtMinuteNode(TramTime.of(10,5), TramDuration.ofMinutes(5));
        mapper.beginTrip(tripId);
        mapper.recordTimeAtMinuteNode(TramTime.of(10,15), TramDuration.ofMinutes(15));
        mapper.leave(Tram, TramDuration.ofMinutes(23), leaveNode);

        mapper.beginWalk(leaveNode, TramDuration.ofMinutes(2));
        mapper.endWalk(walkEndNode, TramDuration.ZERO); // zero since was already able to provide a cost

        mapper.atDestination(TramDuration.getInvalid());

        List<TransportStage<?, ?>> stages = mapper.getStages();
        verifyAll();

        assertEquals(2, stages.size());

        TransportStage<?, ?> stage = stages.getFirst();
        assertEquals(stage.getFirstStation().getId(), navigationRoad.getId());
        assertEquals(stage.getLastStation().getId(), altrincham.getId());
        assertEquals(stage.getTripId(), tripId);
        assertEquals(stage.getFirstDepartureTime(), TramTime.of(10,5));
        assertEquals(stage.getExpectedArrivalTime(), TramTime.of(10,5).plus(TramDuration.ofMinutes(23)));

        // TODO walking stage

    }

    @Test
    void shouldTestWalkFromStartDirect() {

        MyLocation walkStart = MyLocation.create(KnownLocations.betweenAltrinchamAndNavigationRoad.latLong());

        GraphNode beginWalkNode = createMock(GraphNode.class);
        EasyMock.expect(beginWalkNode.hasLabel(GraphLabel.STATION)).andReturn(false);
        EasyMock.expect(beginWalkNode.hasLabel(GraphLabel.QUERY_NODE)).andReturn(true);

        EasyMock.expect(beginWalkNode.getLatLong()).andStubReturn(walkStart.getLatLong());

        GraphNode endWalkNode = createMock(GraphNode.class);
        EasyMock.expect(endWalkNode.hasLabel(GraphLabel.STATION)).andReturn(true);
        EasyMock.expect(endWalkNode.getStationId()).andStubReturn(navigationRoad.getId());

        replayAll();
        mapper.beginWalk(beginWalkNode);
        mapper.endWalk(endWalkNode, TramDuration.ofMinutes(12));

        mapper.atDestination(TramDuration.getInvalid());

        List<TransportStage<?, ?>> stages = mapper.getStages();
        verifyAll();

        assertEquals(1, stages.size());

        TransportStage<?, ?> stage = stages.getFirst();
        assertEquals(walkStart.getId(), stage.getFirstStation().getId());
        assertEquals(stage.getLastStation().getId(), navigationRoad.getId());
        assertEquals(Trip.InvalidId(), stage.getTripId());
        assertEquals(queryTime, stage.getFirstDepartureTime());
        assertEquals(stage.getExpectedArrivalTime(), queryTime.plus(TramDuration.ofMinutes(12)));

    }

    @Disabled("WIP")
    @Test
    void shouldTestWalkAtEndDirect() {

        MyLocation walkDest = MyLocation.create(KnownLocations.betweenAltrinchamAndNavigationRoad.latLong());

        GraphNode beginWalkNode = createMock(GraphNode.class);
        EasyMock.expect(beginWalkNode.hasLabel(GraphLabel.STATION)).andReturn(true);
        EasyMock.expect(beginWalkNode.getStationId()).andStubReturn(navigationRoad.getId());

        GraphNode endWalkNode = createMock(GraphNode.class);
        EasyMock.expect(endWalkNode.hasLabel(GraphLabel.STATION)).andReturn(false);
        EasyMock.expect(endWalkNode.getLatLong()).andReturn(walkDest.getLatLong());

        replayAll();
        mapper.beginWalk(beginWalkNode);
        mapper.endWalk(endWalkNode, TramDuration.ofMinutes(23));

        mapper.atDestination(TramDuration.getInvalid());

        List<TransportStage<?, ?>> stages = mapper.getStages();
        verifyAll();

        assertEquals(1, stages.size());

        TransportStage<?, ?> stage = stages.getFirst();
        assertEquals(navigationRoad.getId(), stage.getFirstStation().getId());
        assertEquals(stage.getLastStation().getId(), walkDest.getId());
        assertEquals(Trip.InvalidId(), stage.getTripId());
        assertEquals(queryTime, stage.getFirstDepartureTime());
        assertEquals(queryTime.plus(TramDuration.ofMinutes(23)), stage.getExpectedArrivalTime());

    }
}
