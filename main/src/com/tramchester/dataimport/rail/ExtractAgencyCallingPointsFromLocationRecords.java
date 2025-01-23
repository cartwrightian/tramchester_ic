package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.BasicScheduleExtraDetails;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.TIPLOCInsert;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
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
    private String currentAtocCode;
    private final List<RailLocationRecord> locations;
    // List not Set, need consistent ordering
    private final List<RailRouteCallingPoints> agencyCallingPoints;

    public ExtractAgencyCallingPointsFromLocationRecords(RailStationRecordsRepository stationRecordsRepository) {
        this.stationRecordsRepository = stationRecordsRepository;
        currentAtocCode = "";
        agencyCallingPoints = new ArrayList<>();
        locations = new ArrayList<>();
    }

    public static List<RailRouteCallingPoints> loadCallingPoints(ProvidesRailTimetableRecords providesRailTimetableRecords,
                                                                 RailStationRecordsRepository stationRecordsRepository) {

        logger.info("Begin extraction of calling points from " + providesRailTimetableRecords.toString());
        ExtractAgencyCallingPointsFromLocationRecords extractor = new ExtractAgencyCallingPointsFromLocationRecords(stationRecordsRepository);

        Stream<RailTimetableRecord> records = providesRailTimetableRecords.load();
        records.forEach(extractor::processRecord);

        logger.info("Finished extraction, loaded " + extractor.agencyCallingPoints.size() + " unique agency calling points records");
        return extractor.agencyCallingPoints;
    }

    private void processRecord(final RailTimetableRecord record) {
        switch (record.getRecordType()) {
            case BasicScheduleExtra -> seenBegin(record);
            case TerminatingLocation -> seenEnd(record);
            case OriginLocation, IntermediateLocation -> seenLocation(record);
            case TiplocInsert -> stationRecordsRepository.add((TIPLOCInsert) record);
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
        if (LocationActivityCode.doesStop(((RailLocationRecord) record).getActivity())) {
            locations.add(locationRecord);
        }
    }

    private void seenEnd(final RailTimetableRecord record) {
        final RailLocationRecord locationRecord = (RailLocationRecord) record;
        locations.add(locationRecord);
        createAgencyCallingPoints();
        currentAtocCode = "";
        locations.clear();
    }

    private void createAgencyCallingPoints() {
        final String atocCode = currentAtocCode;

        final IdFor<Agency> agencyId = Agency.createId(atocCode);

        List<IdFor<Station>> callingPoints = locations.stream().
                filter(RailLocationRecord::doesStop).
                filter(stationRecordsRepository::hasStationRecord).
                map(RailLocationRecord::getTiplocCode).
                map(Station::createId).
                collect(Collectors.toList());

        if (callingPoints.size()>1) {
            agencyCallingPoints.add(new RailRouteCallingPoints(agencyId, callingPoints));
        }

    }



}
