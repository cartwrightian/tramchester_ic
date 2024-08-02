package com.tramchester.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;

public class TimeRangeConfig {
    private TramTime begin;
    private TramTime end;

    public TimeRangeConfig() {

    }

    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getBegin() {
        return begin;
    }

    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getEnd() {
        return end;
    }

    public TimeRange getRange() {
        return TimeRangePartial.of(begin, end);
    }
}
