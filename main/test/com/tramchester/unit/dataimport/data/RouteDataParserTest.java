package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteDataParserTest extends ParserTestCSVHelper<RouteData> {

    @BeforeEach
    void beforeEach() {
        super.before(RouteData.class, "route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color");
        //"route_id,agency_id,route_short_name,route_long_name,route_type");
    }

    // inbound and outbound variations of routes no longer present in data

//    @Test
//    void shouldParseTramRouteOldFormat() {
//        RouteData result = parse("MET:MET4:O:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0");
//
//        assertThat(result.getId()).isEqualTo("MET:MET4:O:");
//        assertThat(result.getShortName()).isEqualTo("MET4");
//        assertThat(result.getLongName()).isEqualTo("Ashton-Under-Lyne - Manchester - Eccles");
//        assertThat(result.getAgencyId()).isEqualTo(Agency.createId("MET"));
//        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.tram);
//    }

    @Test
    void shouldParseTramRouteInboundOldFormat() {
        RouteData result = parse("843,7778482,Blue Line,Eccles - Manchester - Ashton Under Lyne,,1,,,");
                //parse("MET:MET4:I:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0");

        assertThat(result.getAgencyId()).isEqualTo(MutableAgency.METL);
        assertThat(result.getId()).isEqualTo("843"); //"MET:MET4:I:");
        assertThat(result.getShortName()).isEqualTo("Blue Line"); //"MET4");
        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.subway);
    }

    // date variations of routes no longer seem to be present in data

//    @Test
//    void shouldParseTramRoute() {
//        RouteData result = parse("METLBLUE:O:2021-03-08,METL,Blue Line,Eccles - Manchester - Ashton Under Lyne,0");
//
//        assertThat(result.getId()).isEqualTo("METLBLUE:O:2021-03-08");
//        assertThat(result.getShortName()).isEqualTo("Blue Line");
//        assertThat(result.getLongName()).isEqualTo("Eccles - Manchester - Ashton Under Lyne");
//        assertThat(result.getAgencyId()).isEqualTo(TestEnv.MetAgency().getId());
//        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.tram);
//
//    }

//    @Test
//    void shouldParseTramRouteInbound() {
//        RouteData result = parse("METLBLUE:I:2021-03-08,METL,Blue Line,Ashton Under Lyne - Manchester - Eccles,0");
//
//        assertThat(result.getId()).isEqualTo("METLBLUE:I:2021-03-08");
//        assertThat(result.getShortName()).isEqualTo("Blue Line");
//        assertThat(result.getLongName()).isEqualTo("Ashton Under Lyne - Manchester - Eccles");
//        assertThat(result.getAgencyId()).isEqualTo(TestEnv.MetAgency().getId());
//        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.tram);
//    }

    @Test
    void shouldParseBusRoute() {
        RouteData result = parse("572,7778550,588,Leigh - Lowton,,3,,,");
                //"JSC: 588:C:,JSC, 588,\"Leigh - Lowton, Scott Road\",3");

        assertThat(result.getId()).isEqualTo("572");
        assertThat(result.getShortName().trim()).isEqualTo("588");
        assertThat(result.getLongName()).isEqualTo("Leigh - Lowton");
        assertThat(result.getAgencyId()).isEqualTo(Agency.createId("7778550"));
        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.bus);
    }

}