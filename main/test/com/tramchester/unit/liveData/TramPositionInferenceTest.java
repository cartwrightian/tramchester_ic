package com.tramchester.unit.liveData;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.livedata.tfgm.TramPosition;
import com.tramchester.livedata.tfgm.TramPositionInference;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TestRoute;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class TramPositionInferenceTest  extends EasyMockSupport {

    private TramStationAdjacenyRepository adjacencyRepository;
    private RouteReachable routeReachable;
    private TramDepartureRepository departuresRepository;
    private TramPositionInference tramPositionInference;
    private LocalDateTime dateTime;
    private TramDate date;
    private TramTime startTime;
    private TestRoute tramRoute;
    private Station begin;
    private Station end;
    private StationPair pair;
    private TimeRange timeRange;
    private Platform endPlatform;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        TramchesterConfig config = TestEnv.GET();

        departuresRepository = createMock(TramDepartureRepository.class);

        adjacencyRepository = createMock(TramStationAdjacenyRepository.class);
        routeReachable = createMock(RouteReachable.class);
        ClosedStationsRepository closedStationRepository = createMock(ClosedStationsRepository.class);

        tramPositionInference = new TramPositionInference(departuresRepository, adjacencyRepository,
                routeReachable, closedStationRepository, config);

        dateTime = TestEnv.LocalNow();
        date = TramDate.of(dateTime.toLocalDate());
        startTime = TramTime.ofHourMins(dateTime.toLocalTime());

        tramRoute = KnownTramRoute.getBuryManchesterAltrincham(date);
        begin = StPetersSquare.faker().dropOff(tramRoute).dropOffPlatform(1, tramRoute).build();
        end = Deansgate.faker().dropOff(tramRoute).dropOffPlatform(1, tramRoute).build();

        pair = StationPair.of(begin, end);
        timeRange = TimeRange.of(startTime, startTime.plusMinutes(config.getMaxWait()));

        endPlatform = end.getPlatforms().stream().findFirst().get();
    }

    @Test
    void shouldFindDepartureBetweenStations() {
        int costBetweenStations = 3;

        UpcomingDeparture departureFromEnd = new UpcomingDeparture(dateTime.toLocalDate(), end, Bury.fake(), "Due",
                startTime.plusMinutes(costBetweenStations-1), "single", TestEnv.MetAgency(), Tram);
        departureFromEnd.setPlatform(endPlatform);

        EasyMock.expect(adjacencyRepository.getAdjacent(pair.getStationIds(), date, timeRange)).
                andReturn(Duration.ofMinutes(costBetweenStations));

        EasyMock.expect(departuresRepository.forStation(pair.getEnd())).andReturn(Collections.singletonList(departureFromEnd));

        TimeRange cutoffTimeRange = TimeRange.of(startTime, startTime.plusMinutes(costBetweenStations));
        EasyMock.expect(routeReachable.getRoutesFromStartToNeighbour(pair, date, cutoffTimeRange, EnumSet.of(Tram))).
                andReturn(Collections.singletonList(tramRoute.fake()));

        replayAll();
        TramPosition result = tramPositionInference.findBetween(pair, dateTime);
        verifyAll();

        assertTrue(result.hasTrams());

        List<UpcomingDeparture> trams = new ArrayList<>(result.getTrams());
        assertEquals(1, trams.size());

        assertEquals(departureFromEnd, trams.getFirst());

        assertEquals(begin, result.getFirst());
        assertEquals(end, result.getSecond());
        assertEquals(Duration.ofMinutes(costBetweenStations), result.getCost());
    }

    @Test
    void shouldFindDepartureBetweenStationsSecondResolutionOnCostBetweenStations() {
        int costBetweenStationsMins = 3;

        UpcomingDeparture departureFromEnd = new UpcomingDeparture(dateTime.toLocalDate(), end, Bury.fake(), "Due",
                startTime.plusMinutes(costBetweenStationsMins-1), "single", TestEnv.MetAgency(), Tram);
        departureFromEnd.setPlatform(endPlatform);

        EasyMock.expect(adjacencyRepository.getAdjacent(pair.getStationIds(), date, timeRange)).
                andReturn(Duration.ofMinutes(costBetweenStationsMins).plusSeconds(10));

        EasyMock.expect(departuresRepository.forStation(pair.getEnd())).andReturn(Collections.singletonList(departureFromEnd));

        TimeRange cutoffTimeRange = TimeRange.of(startTime, startTime.plusMinutes(costBetweenStationsMins));
        EasyMock.expect(routeReachable.getRoutesFromStartToNeighbour(pair, date, cutoffTimeRange, EnumSet.of(Tram))).
                andReturn(Collections.singletonList(tramRoute.fake()));

        replayAll();
        TramPosition result = tramPositionInference.findBetween(pair, dateTime);
        verifyAll();

        assertTrue(result.hasTrams());

        List<UpcomingDeparture> trams = new ArrayList<>(result.getTrams());
        assertEquals(1, trams.size());

        assertEquals(departureFromEnd, trams.getFirst());

        assertEquals(begin, result.getFirst());
        assertEquals(end, result.getSecond());
        assertEquals(Duration.ofMinutes(costBetweenStationsMins), result.getCost().truncatedTo(ChronoUnit.MINUTES));
    }

    @Test
    void shouldFindDepartureBetweenStationsSecondResolutionOnCostBetweenStationsRoundUp() {
        int costBetweenStationsMins = 3;

        UpcomingDeparture departureFromEnd = new UpcomingDeparture(dateTime.toLocalDate(), end, Bury.fake(), "Due",
                startTime.plusMinutes(costBetweenStationsMins-1), "single", TestEnv.MetAgency(), Tram);
        departureFromEnd.setPlatform(endPlatform);

        EasyMock.expect(adjacencyRepository.getAdjacent(pair.getStationIds(), date, timeRange)).
                andReturn(Duration.ofMinutes(costBetweenStationsMins).plusSeconds(45));

        EasyMock.expect(departuresRepository.forStation(pair.getEnd())).andReturn(Collections.singletonList(departureFromEnd));

        // 3.45 round up to 4 mins
        TimeRange cutoffTimeRange = TimeRange.of(startTime, startTime.plusMinutes(4));
        EasyMock.expect(routeReachable.getRoutesFromStartToNeighbour(pair, date, cutoffTimeRange, EnumSet.of(Tram))).
                andReturn(Collections.singletonList(tramRoute.fake()));

        replayAll();
        TramPosition result = tramPositionInference.findBetween(pair, dateTime);
        verifyAll();

        assertTrue(result.hasTrams());

        List<UpcomingDeparture> trams = new ArrayList<>(result.getTrams());
        assertEquals(1, trams.size());

        assertEquals(departureFromEnd, trams.getFirst());

        assertEquals(begin, result.getFirst());
        assertEquals(end, result.getSecond());
        assertEquals(Duration.ofMinutes(costBetweenStationsMins), result.getCost().truncatedTo(ChronoUnit.MINUTES));
    }

    @Test
    void shouldNotFindDepartureIfOutsideOfTimeWindowBetweenStations() {

        int costBetweenStations = 3;

        UpcomingDeparture departureFromEnd = new UpcomingDeparture(dateTime.toLocalDate(), end, Bury.fake(), "Due",
                startTime.plusMinutes(costBetweenStations+1), "single", TestEnv.MetAgency(), Tram);
        departureFromEnd.setPlatform(endPlatform);

        EasyMock.expect(adjacencyRepository.getAdjacent(pair.getStationIds(), date, timeRange)).
                andReturn(Duration.ofMinutes(costBetweenStations));

        EasyMock.expect(departuresRepository.forStation(pair.getEnd())).andReturn(Collections.singletonList(departureFromEnd));

        TimeRange cutoffTimeRange = TimeRange.of(startTime, startTime.plusMinutes(costBetweenStations));
        EasyMock.expect(routeReachable.getRoutesFromStartToNeighbour(pair, date, cutoffTimeRange, EnumSet.of(Tram))).
                andReturn(Collections.singletonList(tramRoute.fake()));

        replayAll();
        TramPosition result = tramPositionInference.findBetween(pair, dateTime);
        verifyAll();

        assertFalse(result.hasTrams());
        Set<UpcomingDeparture> trams = result.getTrams();
        assertTrue(trams.isEmpty());
    }

}
