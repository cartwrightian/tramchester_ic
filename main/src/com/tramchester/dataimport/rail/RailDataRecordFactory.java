package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.time.ProvidesNow;

import jakarta.inject.Inject;

@LazySingleton
public class RailDataRecordFactory {

    private final int century;
    private final RecordHelper recordHelper;

    @Inject
    public RailDataRecordFactory(final ProvidesNow providesNow, RecordHelper recordHelper) {
        century = Math.floorDiv(providesNow.getDate().getYear(), 100);
        this.recordHelper = recordHelper;
    }

    public RailTimetableRecord createTIPLOC(final String line) {
        return TIPLOCInsert.parse(line, recordHelper);
    }

    public RailTimetableRecord createBasicSchedule(final String line) {
        return BasicSchedule.parse(line, century, recordHelper);
    }

    public RailTimetableRecord createOrigin(final String line, final LocationActivityCode.Parser locationActivityCodeParser) {
        return OriginLocation.parse(line, locationActivityCodeParser, recordHelper);
    }

    public IntermediateLocation createIntermediate(final String line, final LocationActivityCode.Parser locationActivityCodeParser) {
        return IntermediateLocation.parse(line, locationActivityCodeParser, recordHelper);
    }

    public TerminatingLocation createTerminating(final String line, final LocationActivityCode.Parser locationActivityCodeParser) {
        return TerminatingLocation.parse(line, locationActivityCodeParser, recordHelper);
    }

    public RailTimetableRecord createBasicScheduleExtraDetails(final String line) {
        return BasicScheduleExtraDetails.parse(line, recordHelper);
    }
}
