package com.tramchester.unit.domain.places;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.reference.KnownLocations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NaptanRecordTest {

    private IdFor<NaptanRecord> octoCode;
    private IdFor<NPTGLocality> localityId;
    private GridPosition position;
    private LatLong latLong;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        octoCode = NaptanRecord.createId("naptanId");
        localityId = NPTGLocality.createId("localityId");

        position = KnownLocations.nearAltrincham.getGridPosition();
        latLong = KnownLocations.nearAltrincham.latLong();
    }

    @Test
    void shouldHaveExpectedValues() {
        NaptanRecord naptanRecord = new NaptanRecord(octoCode, localityId, "commonName", position, latLong, "suburb", "town",
                NaptanStopType.busCoachTrolleyStopOnStreet, "street", "indicator", false);

        assertEquals(octoCode, naptanRecord.getId());
        assertEquals(localityId, naptanRecord.getLocalityId());
        assertEquals("commonName", naptanRecord.getCommonName());
        assertEquals(position, naptanRecord.getGridPosition());
        assertEquals(latLong, naptanRecord.getLatLong());
        assertEquals("suburb", naptanRecord.getSuburb());
        assertEquals("town", naptanRecord.getTown());
        assertEquals(NaptanStopType.busCoachTrolleyStopOnStreet, naptanRecord.getStopType());
        assertEquals("street", naptanRecord.getStreet());
        assertEquals("indicator", naptanRecord.getIndicator());
        assertFalse(naptanRecord.isLocalityCenter());

        assertEquals("commonName (indicator street), suburb, town", naptanRecord.getDisplayName());
    }

    @Test
    void shouldHaveExpectedDisplayNameNoStreet() {

        NaptanRecord naptanRecord = new NaptanRecord(octoCode, localityId, "commonName", position, latLong, "suburb", "town",
                NaptanStopType.busCoachTrolleyStopOnStreet, "", "indicator", false);

        assertEquals("commonName, suburb, town", naptanRecord.getDisplayName());
    }

    @Test
    void shouldHaveExpectedDisplayNameNoSuburb() {

        NaptanRecord naptanRecord = new NaptanRecord(octoCode, localityId, "commonName", position, latLong, "", "town",
                NaptanStopType.busCoachTrolleyStopOnStreet, "street", "indicator", false);

        assertEquals("commonName (indicator street), town", naptanRecord.getDisplayName());
    }

    @Test
    void shouldHaveExpectedNameBusStationBayNoStreet() {

        NaptanRecord naptanRecord = new NaptanRecord(octoCode, localityId, "commonName", position, latLong, "suburb", "town",
                NaptanStopType.busCoachTrolleyStationBay, "", "indicator", false);

        assertEquals("commonName (indicator), suburb, town", naptanRecord.getDisplayName());


    }
}
