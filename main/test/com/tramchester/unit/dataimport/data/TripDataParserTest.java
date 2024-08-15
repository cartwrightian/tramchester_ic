package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.TripData;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(tripData.getRouteId(), "336");
        assertEquals(tripData.getServiceId(), Service.createId("610"));
        assertEquals(tripData.getTripId(), Trip.createId("693_66"));
        assertEquals(tripData.getHeadsign(), "Bury");
    }


}