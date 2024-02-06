package com.tramchester.unit.dataimport.nptg;

import com.tramchester.dataimport.nptg.xml.NPTGLocalityXMLData;
import com.tramchester.unit.dataimport.ParserTestXMLHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NPTGLocalityXMLDataTest extends ParserTestXMLHelper<NPTGLocalityXMLData> {

    NPTGLocalityXMLDataTest() {
        super(NPTGLocalityXMLData.class);
    }

    @BeforeEach
    void beforeEach() {
        super.before(StandardCharsets.UTF_8);
    }

    @Test
    void shouldParseDataForLocality() throws XMLStreamException, IOException {
        String text = "<NationalPublicTransportGazetter><NptgLocalities>\n" +
                "<NptgLocality CreationDateTime=\"2005-10-05T11:18:07\" ModificationDateTime=\"2021-08-17T11:15:30.703\" Modification=\"revise\" RevisionNumber=\"1\">\n" +
                "<NptgLocalityCode>E0034964</NptgLocalityCode>\n" +
                "<Descriptor>\n" +
                "<LocalityName xml:lang=\"EN\">Amesbury</LocalityName>\n" +
                "<Qualify>\n" +
                "<QualifierName xml:lang=\"en\">Bath &amp; North East Somerset</QualifierName>\n" +
                "</Qualify>\n" +
                "</Descriptor>\n" +
                "<ParentNptgLocalityRef CreationDateTime=\"2005-10-05T10:49:25\" ModificationDateTime=\"2005-10-05T10:49:25\" RevisionNumber=\"0\">N0060403</ParentNptgLocalityRef>"+
                "<AdministrativeAreaRef>001</AdministrativeAreaRef>\n" +
                "<NptgDistrictRef>310</NptgDistrictRef>\n" +
                "<SourceLocalityType>Lo</SourceLocalityType>\n" +
                "<Location>\n" +
                "<Translation>\n" +
                "<Easting>365491</Easting>\n" +
                "<Northing>158651</Northing>\n" +
                "<Longitude>-2.496644</Longitude>\n" +
                "<Latitude>51.3259</Latitude>\n" +
                "</Translation>\n" +
                "</Location>\n" +
                "</NptgLocality>\n" +
                "</NptgLocalities></NationalPublicTransportGazetter>";

        NPTGLocalityXMLData data = super.parseFirstOnly(text);

        assertNotNull(data);

        assertEquals("E0034964", data.getNptgLocalityCode());
        assertEquals("Amesbury", data.getLocalityName());

        assertEquals("365491", data.getEasting());
        assertEquals("158651", data.getNorthing());

        assertEquals("51.3259", data.getLatitude());
        assertEquals("-2.496644", data.getLongitude());

        assertEquals("N0060403", data.getParentLocalityRef());
    }

}
