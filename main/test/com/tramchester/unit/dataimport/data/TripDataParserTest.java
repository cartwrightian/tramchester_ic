package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.TripData;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TripDataParserTest extends ParserTestCSVHelper<TripData> {

    @BeforeEach
    void beforeEach() {
        super.before(TripData.class, "route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id");
                //"route_id,service_id,trip_id,trip_headsign");
    }

    @Test
    void shouldParseMetrolink() {

        TripData tripData = parse("336,610,693_66,Bury,2140,1,,693_23");
                //"MET:MET1:I:,Serv000001,Trip000001,Bury");

        assertThat(tripData.getRouteId()).isEqualTo("336");
        assertThat(tripData.getServiceId()).isEqualTo(Service.createId("610"));
        assertThat(tripData.getTripId()).isEqualTo(Trip.createId("693_66"));
        assertThat(tripData.getHeadsign()).isEqualTo("Bury");
    }

    // no real difference now

//    @Test
//    void shouldParseOther() {
//        TripData tripData = parse("CBL: 157:I:,Serv000153,Trip004334,Garswood");
//
//        assertThat(tripData.getRouteId()).isEqualTo("CBL:157:I:");
//        assertThat(tripData.getServiceId()).isEqualTo(Service.createId("Serv000153"));
//        assertThat(tripData.getTripId()).isEqualTo(Trip.createId("Trip004334"));
//        assertThat(tripData.getHeadsign()).isEqualTo("Garswood");
//    }

}