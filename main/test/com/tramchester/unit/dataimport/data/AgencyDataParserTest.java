package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.AgencyData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgencyDataParserTest extends ParserTestCSVHelper<AgencyData> {

    @BeforeEach
    void beforeEach() {
        super.before(AgencyData.class, "agency_id,agency_name,agency_url,agency_timezone,agency_lang,agency_phone,agency_fare_url,agency_email,agency_noc");
        //"agency_id,agency_name,agency_url,agency_timezone,agency_lang");
    }

    @Test
    void shouldParseAnAgency() {
        AgencyData agencyData = parse("7778465,Bee Network,https://www.beenetwork.com,Europe/London,,,,,BNSM");
                // "GMS,Stagecoach Manchester,http://www.tfgm.com,Europe/London,en");

        assertEquals(Agency.createId("7778465"), agencyData.getId());
        assertEquals("Bee Network", agencyData.getName());
    }
}
