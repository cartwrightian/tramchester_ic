package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RecordHelper;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PhysicalStationRecordTest {

    private RecordHelper recordHelper;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        recordHelper = new RecordHelper();
    }

    @Test
    void shouldParseBasicRecordCorrectly() {
        String text = "A    DERBY                         2DRBY   DBY   DBY14362 63356 6";

        PhysicalStationRecord result = PhysicalStationRecord.parse(text, recordHelper);

        assertEquals("DERBY", result.getName());
        assertEquals("DRBY", result.getTiplocCode());
        assertEquals(4362, result.getEasting());
        assertEquals(3356, result.getNorthing());
        assertEquals(6, result.getMinChangeTime());
        assertEquals("DBY", result.getCRS());

        assertEquals(RailInterchangeType.Medium, result.getRailInterchangeType());
    }

    @Test
    void shouldParseMinChangeTimeCorrectly() {
        String edin = "A    EDINBURGH                     3EDINBUREDB   EDB13259 6673910";
        PhysicalStationRecord resultA = PhysicalStationRecord.parse(edin, recordHelper);
        assertTrue(resultA.isMinChangeTimeValid());
        assertEquals(10, resultA.getMinChangeTime());

        String missing = "A    EDINBURGH                     3EDINBUREDB   EDB13259 66739  ";
        PhysicalStationRecord resultB = PhysicalStationRecord.parse(missing, recordHelper);
        assertFalse(resultB.isMinChangeTimeValid());

        String invalid = "A    EDINBURGH                     3EDINBUREDB   EDB13259 66739XX";
        PhysicalStationRecord resultC = PhysicalStationRecord.parse(invalid, recordHelper);
        assertFalse(resultC.isMinChangeTimeValid());
    }

    @Test
    void shouldParseRecordsWithMissingGridCorrectly() {
        String text = "A    BALLINASLOE (CIE              0CATZBSGBSG   BSG00000E00000 5";

        PhysicalStationRecord result = PhysicalStationRecord.parse(text, recordHelper);

        assertEquals("BALLINASLOE (CIE", result.getName());
        assertEquals("CATZBSG", result.getTiplocCode());
        assertEquals(Integer.MIN_VALUE, result.getEasting());
        assertEquals(Integer.MIN_VALUE, result.getNorthing());
        assertEquals(RailInterchangeType.None, result.getRailInterchangeType());
    }

    @Test
    void shouldParseFileSpec() {
        String text = "A                             FILE-SPEC=05 1.00 12/11/21 18.10.25   193";

        PhysicalStationRecord result = PhysicalStationRecord.parse(text, recordHelper);

        assertEquals("",result.getName());
        assertEquals(Integer.MAX_VALUE, result.getNorthing());
        assertEquals(Integer.MAX_VALUE, result.getEasting());
    }

    @Test
    void shouldParseKeyGardens() {
        String text = "A    KEW GARDENS                   0KEWGRDNKWG   KWG15192 61768 5";

        PhysicalStationRecord result = PhysicalStationRecord.parse(text, recordHelper);

        assertEquals("KEW GARDENS", result.getName());
        assertEquals("KEWGRDN", result.getTiplocCode());
        assertEquals(5192, result.getEasting());
        assertEquals(1768, result.getNorthing());
        assertEquals("KWG", result.getCRS());
        assertEquals(RailInterchangeType.None, result.getRailInterchangeType());

    }

}
