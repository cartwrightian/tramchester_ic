package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.BasicScheduleExtraDetails;
import com.tramchester.dataimport.rail.records.Line;
import com.tramchester.dataimport.rail.records.RecordHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicScheduleExtraDetailsTest {

    private RecordHelper recordHelper;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        recordHelper = new RecordHelper();
    }

    @Test
    void shouldParseCorrectly() {
        Line line = Line.of("BX         EMYEM813500");

        BasicScheduleExtraDetails extraDetails = BasicScheduleExtraDetails.parse(line, recordHelper);

        assertEquals("EM", extraDetails.getAtocCode());
        assertEquals("EM813500", extraDetails.getRetailServiceID());
    }
}
