package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.ShortTermPlanIndicator;
import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Supports route id creation by preloading calling points for each service in the timetable into a
 * set of calling point records
 */
public class ExtractAgencyCallingPointsFromLocationRecords {
    private static final Logger logger = LoggerFactory.getLogger(ExtractAgencyCallingPointsFromLocationRecords.class);

    private final RailStationRecordsRepository stationRecordsRepository;
    private final List<RailLocationRecord> locations;
    // List not Set, need consistent ordering
    private final List<RailRouteCallingPoints> agencyCallingPoints;

    private String currentAtocCode;
    private TransportMode transportMode;

    public ExtractAgencyCallingPointsFromLocationRecords(RailStationRecordsRepository stationRecordsRepository) {
        this.stationRecordsRepository = stationRecordsRepository;
        currentAtocCode = "";
        transportMode = TransportMode.Unknown;
        agencyCallingPoints = new ArrayList<>();
        locations = new ArrayList<>();
    }

    public static List<RailRouteCallingPoints> loadCallingPoints(final ProvidesRailTimetableRecords providesRailTimetableRecords,
                                                                 final RailStationRecordsRepository stationRecordsRepository) {

        logger.info("Begin extraction of calling points from " + providesRailTimetableRecords.toString());
        final ExtractAgencyCallingPointsFromLocationRecords extractor = new ExtractAgencyCallingPointsFromLocationRecords(stationRecordsRepository);

        final Stream<RailTimetableRecord> records = providesRailTimetableRecords.load();
        records.forEach(extractor::processRecord);

        logger.info("Finished extraction, loaded " + extractor.agencyCallingPoints.size() + " unique agency calling points records");
        return extractor.agencyCallingPoints;
    }

    private void processRecord(final RailTimetableRecord record) {
        switch (record.getRecordType()) {
            case BasicSchedule -> seenBasicSchedule(record);
            case BasicScheduleExtra -> seenBegin(record);
            case OriginLocation, IntermediateLocation -> seenLocation(record);
            case TerminatingLocation -> seenEnd(record);
            case TiplocInsert -> stationRecordsRepository.add((TIPLOCInsert) record);
        }
    }

    private void seenBasicSchedule(final RailTimetableRecord record) {
        final BasicSchedule basicSchedule = (BasicSchedule) record;
        if (basicSchedule.getSTPIndicator()!= ShortTermPlanIndicator.Cancellation) {
            transportMode = RailTransportModeMapper.getModeFor(basicSchedule);
        } else {
            transportMode = TransportMode.Unknown;
        }
    }

    private void seenBegin(final RailTimetableRecord record) {
        if (!currentAtocCode.isEmpty()) {
            throw new RuntimeException("Unexpected state, was still processing for " + currentAtocCode + " at " + record);
        }

        final BasicScheduleExtraDetails extraDetails = (BasicScheduleExtraDetails) record;
        currentAtocCode = extraDetails.getAtocCode();
    }

    private void seenLocation(final RailTimetableRecord record) {
        final RailLocationRecord locationRecord = (RailLocationRecord) record;

        if (locationRecord.doesStop()) {
            locations.add(locationRecord);
        }
    }

    private void seenEnd(final RailTimetableRecord record) {
        final RailLocationRecord locationRecord = (RailLocationRecord) record;
        locations.add(locationRecord);
        createAgencyCallingPoints();
        currentAtocCode = "";
        transportMode = TransportMode.Unknown;
        locations.clear();
    }

    private void createAgencyCallingPoints() {
        final String atocCode = currentAtocCode;

        final IdFor<Agency> agencyId = Agency.createId(atocCode);

        if (transportMode==TransportMode.Unknown) {
            throw new RuntimeException("Unknown mode");
        }

        List<IdFor<Station>> callingPoints = locations.stream().
                filter(RailLocationRecord::doesStop).
                filter(stationRecordsRepository::hasStationRecord).
                map(RailLocationRecord::getTiplocCode).
                map(Station::createId).
                collect(Collectors.toList());

        if (callingPoints.size()>1) {
            agencyCallingPoints.add(new RailRouteCallingPoints(agencyId, callingPoints, transportMode));
        }

    }



}
