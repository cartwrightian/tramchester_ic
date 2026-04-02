package com.tramchester.dataimport.rail.records;

import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.time.TramTime;

public interface RailLocationRecord extends RailTimetableRecord {
    String getTiplocCode();
    TramTime getArrival();
    TramTime getDeparture();
    String getPlatform();

    ImmutableEnumSet<LocationActivityCode> getActivity();

    boolean isOrigin();
    boolean isTerminating();
    boolean doesStop();

    TramTime getPassingTime();
}
