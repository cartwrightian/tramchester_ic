package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.domain.time.ProvidesNow;

import jakarta.inject.Inject;

@LazySingleton
public class RailDataRecordFactory {

    private final int century;

    @Inject
    public RailDataRecordFactory(final ProvidesNow providesNow) {
        century = Math.floorDiv(providesNow.getDate().getYear(), 100);
    }

    public RailTimetableRecord createTIPLOC(final String line) {
        return TIPLOCInsert.parse(line);
    }

    public RailTimetableRecord createBasicSchedule(final String line) {
        return BasicSchedule.parse(line, century);
    }

    public RailTimetableRecord createOrigin(final String line) {
        return OriginLocation.parse(line);
    }

    public IntermediateLocation createIntermediate(final String line) {
        return IntermediateLocation.parse(line);
    }

    public TerminatingLocation createTerminating(final String line) {
        return TerminatingLocation.parse(line);
    }

    public RailTimetableRecord createBasicScheduleExtraDetails(final String line) {
        return BasicScheduleExtraDetails.parse(line);
    }
}
