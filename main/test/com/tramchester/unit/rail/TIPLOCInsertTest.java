package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.TIPLOCInsert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TIPLOCInsertTest {

    @Test
    void shouldParseSimpleRecord() {
        String text = "TIAACHEN 00081601LAACHEN                    00005   0";

        TIPLOCInsert result = TIPLOCInsert.parse(text);

        assertEquals("AACHEN", result.getTiplocCode());
    }

    @Test
    void shouldHaveRecordTruncated() {
        String text = "TIBATRSH 24528866ABATTERSEA PIER STAFF HALT 87239   0";

        TIPLOCInsert result = TIPLOCInsert.parse(text);

        assertEquals("BATRSH", result.getTiplocCode());
        assertEquals("BATTERSEA PIER STAFF HALT", result.getName());
        assertFalse(result.isUseful());
//        assertEquals("xx", result.getCRS());
    }

    @Test
    void shouldParseRecordFromFile() {

        // TIBATRSPJ48528862ZBATTERSEA PIER JN.        87199   0
        // 01234567890123456789012345678901234567890123456789012
        // 0         1         2         3         4         5

        String text = "TIBATRSPJ48528862ZBATTERSEA PIER JN.        87199   0";

        TIPLOCInsert result = TIPLOCInsert.parse(text);

        assertEquals("BATRSPJ", result.getTiplocCode());
        assertEquals("BATTERSEA PIER JN.", result.getName());
        assertFalse(result.isUseful());
    }

    @Test
    void shouldparselondonUnderground() {
        // TITRNHMGN16073400DTURNHAM GREEN LT          87130   0ZTUTURNHAM GREEN LT
        // 012345678901234567890123456789012345678901234567890123456789012345678901
        // 0         1         2         3         4         5

        String text = "TITRNHMGN16073400DTURNHAM GREEN LT          87130   0ZTUTURNHAM GREEN LT";

        TIPLOCInsert result = TIPLOCInsert.parse(text);

        assertEquals("TRNHMGN", result.getTiplocCode());
        assertEquals("TURNHAM GREEN LT", result.getName());
        assertEquals("ZTU", result.getCRS());
        assertTrue(result.isUseful());

    }
}
