package com.tramchester.unit.dataimport.rail;

import com.tramchester.dataimport.rail.records.Line;
import com.tramchester.dataimport.rail.records.RecordHelper;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecordHelperTest {

    private RecordHelper recordHelper;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        recordHelper = new RecordHelper();
    }

    @Test
    void shouldParseTime() {
        Line line = Line.of("xxxx1145yyyy");
        TramTime result = recordHelper.extractTime(line, 4);

        assertEquals(TramTime.of(11,45), result);
        // cached
        assertEquals(TramTime.of(11,45),  recordHelper.extractTime(line, 4));
    }

    @Test
    void shouldParseTimeAsLine() {
        Line line = Line.of("xxxx1145yyyy");
        TramTime result = recordHelper.extractTime(line, 4);

        assertEquals(TramTime.of(11,45), result);
        // cached
        assertEquals(TramTime.of(11,45),  recordHelper.extractTime(line, 4));
    }

    @Test
    void shouldParseDate() {
        String text = "xxxx220513yyyyy";

        int century = 20;

        Line line = new Line(text);
        TramDate result = recordHelper.extractTramDate(line, 4, century);

        assertEquals(TramDate.of(2022, 5, 13), result);
    }

    @Test
    void shouldExtractText() {
        Line text = Line.of("ABCD12345vwxyz");

        assertEquals("ABCD", recordHelper.extractToString(text, 1, 4));

        assertEquals("12345", recordHelper.extractToString(text, 5, 9));

        assertEquals("vwxyz", recordHelper.extractToString(text, 10, 14));

    }

    @Test
    void shouldParseLocationActivityCode() {
        Line line = Line.of("LTLILBDGE 2030 2030      TF               ");

        final ImmutableEnumSet<LocationActivityCode> activity = recordHelper.parseLocationActivityCode(line, 26, 37);

        assertEquals(1, activity.size());
        assertTrue(activity.contains(LocationActivityCode.TrainFinishes));
    }

    @Test
    void shouldExtractLocationCode() {
        Line line = Line.of("LTLILBDGE 2030 2030      TF               ");

        final String tiplocCode = recordHelper.extractToString(line, 3, 10);

        assertEquals("LILBDGE", tiplocCode);
    }

    @Test
    void shouldExtractLocationCodeWithTrailingSpaces() {
        Line line = Line.of("LTUPMNLT 21022H1023      TF");

        final String tiplocCode = recordHelper.extractToString(line, 3, 9);

        assertEquals("UPMNLT", tiplocCode);
    }

    @Test
    void shouldExtractToEmptyString() {
        Line line = Line.of("LTWLWYNGC 1918 19184     TF");

        final String path = recordHelper.extractToString(line,23, 25);
        assertTrue(path.isEmpty());
    }

    @Test
    void shouldExtract2Characters() {
        Line line = Line.of("BX         EMYEM813500");

        String result = recordHelper.extractToString(line, 12, 13);
        assertEquals("EM", result);
    }

}
