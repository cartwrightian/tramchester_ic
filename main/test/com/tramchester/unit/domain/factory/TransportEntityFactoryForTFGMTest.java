package com.tramchester.unit.domain.factory;

import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.factory.TransportEntityFactoryForTFGM.getStationIdFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransportEntityFactoryForTFGMTest {

    @Test
    void testShouldFormIdByRemovingPlatformForTramStopIfRequired() {
        assertEquals(Station.createId("9400ZZid"), getStationIdFor("9400ZZid1"));

        assertEquals(Station.createId("9400XXid1"), getStationIdFor("9400XXid1"));

    }

    @Test
    void shouldRemoveStationFromPlatformNumberIfPresent() {
        IdFor<Station> stationId = TramStations.ImperialWarMuseum.getId();
        String platformNumber = "9400ZZMAIWM1";
        PlatformId platformId = TransportEntityFactoryForTFGM.createPlatformId(stationId, platformNumber);

        assertEquals("1", platformId.getNumber());
    }

    @Test
    void shouldNotRemoveStationFromPlatformNumberIfNoMatch() {
        IdFor<Station> stationId = TramStations.ImperialWarMuseum.getId();
        String platformNumber = "42";
        PlatformId platformId = TransportEntityFactoryForTFGM.createPlatformId(stationId, platformNumber);

        assertEquals("42", platformId.getNumber());
    }
}
