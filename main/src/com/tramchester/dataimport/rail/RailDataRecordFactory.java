package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.domain.time.ProvidesNow;
import jakarta.inject.Inject;

@LazySingleton
public class RailDataRecordFactory {

    private final int century;
    private final RecordHelper recordHelper;

    @Inject
    public RailDataRecordFactory(final ProvidesNow providesNow, final RecordHelper recordHelper) {
        century = Math.floorDiv(providesNow.getDate().getYear(), 100);
        this.recordHelper = recordHelper;
    }

    public RailTimetableRecord createTIPLOC(final Line line) {
        return TIPLOCInsert.parse(line, recordHelper);
    }

    public RailTimetableRecord createBasicSchedule(final Line line) {
        return BasicSchedule.parse(line, century, recordHelper);
    }

    public RailTimetableRecord createOrigin(final Line line) {
        return OriginLocation.parse(line, recordHelper);
    }

    public IntermediateLocation createIntermediate(final Line line) {
        return IntermediateLocation.parse(line, recordHelper);
    }

    public TerminatingLocation createTerminating(final Line line) {
        return TerminatingLocation.parse(line, recordHelper);
    }

    public RailTimetableRecord createBasicScheduleExtraDetails(final Line line) {
        return BasicScheduleExtraDetails.parse(line, recordHelper);
    }
}
