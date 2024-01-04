package com.tramchester.unit.dataimport.nptg;

import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.geo.GridPosition;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParseNPTGDataTest extends ParserTestCSVHelper<NPTGData> {

    @BeforeEach
    void beforeEachTestRuns() {
        String header = "NptgLocalityCode,LocalityName,LocalityNameLang,ShortName,ShortNameLang,QualifierName,QualifierNameLang," +
                "QualifierLocalityRef,QualifierDistrictRef,ParentLocalityName,ParentLocalityNameLang,AdministrativeAreaCode,NptgDistrictCode," +
                "SourceLocalityType,GridType,Easting,Northing,CreationDateTime,ModificationDateTime,RevisionNumber,Modification";
        super.before(NPTGData.class, header);
    }

    @Test
    void shouldParseCodeWithNoAdminArea() {
        String text = "N0081052,Openshaw Park,EN,,,Manchester,,,,Bury,EN,083,244,Add,,381640,410542," +
                "2018-08-31T10:45:47.730,2019-04-17T12:16:36.723,1,revise";

        NPTGData item = super.parse(text);
        assertEquals("N0081052", item.getNptgLocalityCode());
        assertEquals("Openshaw Park", item.getLocalityName());
        assertEquals("Bury", item.getParentLocalityName());

        GridPosition gridPosition = item.getGridPosition();
        assertTrue(gridPosition.isValid());
        assertEquals(381640L, gridPosition.getEastings());
        assertEquals(410542L, gridPosition.getNorthings());
    }



}
