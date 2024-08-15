package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteDataParserTest extends ParserTestCSVHelper<RouteData> {

    @BeforeEach
    void beforeEach() {
        super.before(RouteData.class, "route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color");
        //"route_id,agency_id,route_short_name,route_long_name,route_type");
    }

    @Test
    void shouldParseTramRouteInboundOldFormat() {
        RouteData result = parse("843,7778482,Blue Line,Eccles - Manchester - Ashton Under Lyne,,1,,,");
                //parse("MET:MET4:I:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0");

        assertEquals(result.getAgencyId(), MutableAgency.METL);
        assertEquals(result.getId(), "843"); //"MET:MET4:I:");
        assertEquals(result.getShortName(), "Blue Line"); //"MET4");
        assertEquals(result.getRouteType(), GTFSTransportationType.subway);
    }

    @Test
    void shouldParseBusRoute() {
        RouteData result = parse("572,7778550,588,Leigh - Lowton,,3,,,");
                //"JSC: 588:C:,JSC, 588,\"Leigh - Lowton, Scott Road\",3");

        assertEquals(result.getId(), "572");
        assertEquals(result.getShortName().trim(), "588");
        assertEquals(result.getLongName(), "Leigh - Lowton");
        assertEquals(result.getAgencyId(), Agency.createId("7778550"));
        assertEquals(result.getRouteType(), GTFSTransportationType.bus);
    }

}