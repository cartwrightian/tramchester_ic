package com.tramchester.unit.repository.rail;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.ProvidesRailTimetableRecords;
import com.tramchester.dataimport.rail.RailRouteIDBuilder;
import com.tramchester.dataimport.rail.RailTransportModeMapper;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.dataimport.rail.records.reference.ShortTermPlanIndicator;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.dataimport.rail.repository.RailRouteIds;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ReportsCacheStats;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RailRouteIdsTest extends EasyMockSupport {

    private BasicSchedule basicSchedule;
    private BasicSchedule basicScheduleBus;
    private BasicScheduleExtraDetails extraDetails;
    private OriginLocation originLocation;
    private IntermediateLocation intermediateLocation;
    private TerminatingLocation terminatingLocation;

    private IdFor<Station> stationAId;
    private IdFor<Station> stationBId;
    private IdFor<Station> stationCId;
    private IdFor<Agency> agencyId;
    private RailRouteIDBuilder railRouteIdBuilder;
    private RailStationRecordsRepository stationRecords;
    private CacheMetrics cacheMetrics;
    private TramchesterConfig config;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        TramDate startDate = TestEnv.testDay();
        TramDate endDate = startDate.plusDays(30);

        basicSchedule = new BasicSchedule(RailRecordTransactionType.New, "trainId", startDate, endDate,
                EnumSet.allOf(DayOfWeek.class), ShortTermPlanIndicator.Permanent, "headCode", TrainStatus.PassengerAndParcels,
                TrainCategory.ExpressPassenger);

        basicScheduleBus = new BasicSchedule(RailRecordTransactionType.New, "trainId", startDate, endDate,
                EnumSet.allOf(DayOfWeek.class), ShortTermPlanIndicator.Permanent, "headCode", TrainStatus.Bus,
                TrainCategory.BusReplacement);

        extraDetails = new BasicScheduleExtraDetails("NT", "serviceId");

        TramTime deptTime = TramTime.of(9, 2);
        originLocation = new OriginLocation("TIPLOC1", deptTime,
                "1", "lineA", ImmutableEnumSet.of(LocationActivityCode.StopsToTakeUpPassengers));

        TramTime arrivalAtIntermediate = deptTime.plusMinutes(10);
        intermediateLocation = new IntermediateLocation("TIPLOC2", arrivalAtIntermediate,
                arrivalAtIntermediate.plusMinutes(2), arrivalAtIntermediate,
                arrivalAtIntermediate.plusMinutes(2), "2", arrivalAtIntermediate,
                ImmutableEnumSet.of(LocationActivityCode.StopsToTakeUpAndSetDownPassengers));

        TramTime atTerminating = arrivalAtIntermediate.plusMinutes(30);
        terminatingLocation = new TerminatingLocation("TIPLOC3", atTerminating,
                "3", "path", ImmutableEnumSet.of(LocationActivityCode.StopsToSetDownPassengers));

        stationAId = Station.createId(originLocation.getTiplocCode());
        stationBId = Station.createId(intermediateLocation.getTiplocCode());
        stationCId = Station.createId(terminatingLocation.getTiplocCode());

        agencyId = Agency.createId(extraDetails.getAtocCode());

        railRouteIdBuilder = new RailRouteIDBuilder();

        stationRecords = createMock(RailStationRecordsRepository.class);
        cacheMetrics = createMock(CacheMetrics.class);

        config = createMock(TramchesterConfig.class);
        EasyMock.expect(config.hasRailConfig()).andStubReturn(true);

        cacheMetrics.register(EasyMock.isA(ReportsCacheStats.class));
        EasyMock.expectLastCall();
    }

    @Test
    void shouldCreateExpectedIdAndCallingPoints() {

        EasyMock.expect(stationRecords.hasStationRecord(originLocation)).andReturn(true);
        EasyMock.expect(stationRecords.hasStationRecord(intermediateLocation)).andReturn(true);
        EasyMock.expect(stationRecords.hasStationRecord(terminatingLocation)).andReturn(true);

        ProvidesRailTimetableRecords providesRecords = () ->
                Stream.of(basicSchedule, extraDetails, originLocation, intermediateLocation, terminatingLocation);

        replayAll();
        RailRouteIds railRouteIds = new RailRouteIds(stationRecords, providesRecords, railRouteIdBuilder, config,
                cacheMetrics);
        railRouteIds.start();
        List<RailRouteIds.RailRouteCallingPointsWithRouteId> results = new ArrayList<>(railRouteIds.getCallingPointsFor(agencyId));
        verifyAll();

        assertEquals(1, results.size());

        RailRouteIds.RailRouteCallingPointsWithRouteId result = results.getFirst();

        assertEquals(3, result.numberCallingPoints());

        StationIdPair beginEnd = StationIdPair.of(stationAId, stationCId);

        assertEquals(beginEnd, result.getBeginEnd());
        RailRouteCallingPoints callingPoints = result.getCallingPoints();

        assertEquals(agencyId, callingPoints.getAgencyId());
        assertEquals(3, callingPoints.numberCallingPoints());
        assertEquals(beginEnd, callingPoints.getBeginEnd());
        assertEquals(List.of(stationAId, stationBId, stationCId), callingPoints.getCallingPoints());

        assertEquals(Train, callingPoints.getMode());

        RailRouteId routeId = result.getRouteId();
        assertEquals(agencyId, routeId.getAgencyId());
        assertEquals(stationAId, routeId.getBegin());
        assertEquals(stationCId, routeId.getEnd());
        assertEquals(1, routeId.getIndex());
    }

    @Test
    void shouldHaveExpectedModesForTestData() {
        assertEquals(Train, RailTransportModeMapper.getModeFor(basicSchedule));
        assertEquals(RailReplacementBus, RailTransportModeMapper.getModeFor(basicScheduleBus));
    }

    @Test
    void shouldCreateExpectedIdAndCallingPointsWhenReplacementBusPresent() {

        EasyMock.expect(stationRecords.hasStationRecord(originLocation)).andStubReturn(true);
        EasyMock.expect(stationRecords.hasStationRecord(intermediateLocation)).andStubReturn(true);
        EasyMock.expect(stationRecords.hasStationRecord(terminatingLocation)).andStubReturn(true);

        ProvidesRailTimetableRecords providesRecords = () ->
                Stream.of(basicSchedule, extraDetails, originLocation, intermediateLocation, terminatingLocation,
                        basicScheduleBus, extraDetails, originLocation, intermediateLocation, terminatingLocation);

        replayAll();
        RailRouteIds railRouteIds = new RailRouteIds(stationRecords, providesRecords, railRouteIdBuilder, config,
                cacheMetrics);
        railRouteIds.start();
        List<RailRouteIds.RailRouteCallingPointsWithRouteId> results = new ArrayList<>(railRouteIds.getCallingPointsFor(agencyId));
        verifyAll();

        assertEquals(2, results.size());

        RailRouteIds.RailRouteCallingPointsWithRouteId first = results.getFirst();
        RailRouteIds.RailRouteCallingPointsWithRouteId second = results.getLast();

        assertEquals(3, first.numberCallingPoints());
        assertEquals(3, second.numberCallingPoints());

        assertEquals(Train, first.getCallingPoints().getMode());
        assertEquals(RailReplacementBus, second.getCallingPoints().getMode());

        assertNotEquals(first.getRouteId(), second.getRouteId());

        assertEquals(1, first.getRouteId().getIndex(), "unexpected index for " + first.getRouteId());
        assertEquals(2, second.getRouteId().getIndex(), "unexpected index for " + second.getRouteId());

    }

}
