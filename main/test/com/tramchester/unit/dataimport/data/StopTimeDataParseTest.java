package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopTimeDataParseTest extends ParserTestCSVHelper<StopTimeData> {

    @BeforeEach
    void beforeEachTestRuns() {
        String header = "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,timepoint";
        //String header = "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled";
        //"trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type";
        super.before(StopTimeData.class, header);
    }

    @Test
    void shouldParseStop() {
        String stop = "1679_1,06:41:00,06:42:00,103701,1,,0,1,";
                //"Trip000001,06:41:00,06:41:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getTripId()).isEqualTo("1679_1");
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.of(6, 41));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.of(6, 42));
        assertThat(stopTimeData.getStopId()).isEqualTo("103701");
        assertThat(stopTimeData.getStopSequence()).isEqualTo(1);
        assertThat(stopTimeData.getPickupType()).isEqualTo(GTFSPickupDropoffType.Regular);
        assertThat(stopTimeData.getDropOffType()).isEqualTo(GTFSPickupDropoffType.None);

        assertTrue(stopTimeData.isValid());
    }

    @Test
    void shouldParseStopSingleDigitHour() {
        // this occurs often in tgfm data
        String stop = "1679_1,6:41:00,6:42:00,103701,1,,0,1,";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getTripId()).isEqualTo("1679_1");
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.of(6, 41));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.of(6, 42));
        assertThat(stopTimeData.getStopId()).isEqualTo("103701");
        assertThat(stopTimeData.getStopSequence()).isEqualTo(1);

        assertTrue(stopTimeData.isValid());

    }

    @Test
    void shouldParseButReturnInvalidIfArrivalTimeIsInvalid() {
        String stop = "Trip000001,12:99:00,6:42:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertFalse(stopTimeData.isValid());
    }

    @Test
    void shouldParseButReturnInvalidIfDepartureTimeIsInvalid() {
        String stop = "Trip000001,12:09:00,88:42:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertFalse(stopTimeData.isValid());
    }

    @Test
    void shouldCopeWith24TimeFormatInData() {
        String stop = "Trip000001,24:00:00,24:00:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.nextDay(0,0));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.nextDay(0,0));

        assertTrue(stopTimeData.isValid());

    }

    @Test
    void shouldCopeWith25TimeFormatInData() {
        String stop = "Trip000001,25:05:00,25:07:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.nextDay(1,5));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.nextDay(1,7));

        assertTrue(stopTimeData.isValid());

    }

    @Test
    void shouldDealWithBadStopTimeData() {
        String stop = "3430_51,,,141053,28,,0,0,,0";

        StopTimeData stopTimeData = parse(stop);

        assertFalse(stopTimeData.isValid());
    }

}