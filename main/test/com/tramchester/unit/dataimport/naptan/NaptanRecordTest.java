package com.tramchester.unit.dataimport.naptan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NaptanRecordTest {
    @Test
    void shouldRoundTrip() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        IdFor<NaptanRecord> id = StringIdFor.convert(TramStations.Bury.getId(), NaptanRecord.class);
        IdFor<NPTGLocality > localityId = NPTGLocality.createId("nptgId1234");
        String commonName = "theCommonName";
        GridPosition gridPosition= KnownLocations.atMancArena.getGridPosition();
        LatLong latlong = KnownLocations.atMancArena.latLong();
        String suburb = "inSurburbia";
        String town = "Manchester";
        NaptanStopType stopType = NaptanStopType.airportEntrance;
        String street = "Coronation Street";
        String indicator = "indicatorZ";
        boolean localityCenter = false;

        IdFor<Station> railStationId = RailStationIds.Belper.getId();
        NaptanRecord original = new NaptanRecord(id, localityId, commonName, gridPosition, latlong, suburb, town,
                stopType, street, indicator, localityCenter, railStationId);

        String text = objectMapper.writeValueAsString(original);

        NaptanRecord result = objectMapper.readValue(text, NaptanRecord.class);

        assertEquals(id, result.getId());
        assertEquals(localityId, result.getLocalityId());
        assertEquals(commonName, result.getCommonName());
        assertEquals(gridPosition, result.getGridPosition());
        assertEquals(latlong, result.getLatLong());
        assertEquals(suburb, result.getSuburb());
        assertEquals(town, result.getTown());
        assertEquals(stopType, result.getStopType());
        assertEquals(street, result.getStreet());
        assertEquals(indicator, result.getIndicator());
        assertEquals(localityCenter, result.isLocalityCenter());
        assertEquals(railStationId, result.getRailStationId());

    }

    @Test
    void shouldRoundTripInvalidRailStation() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        IdFor<NaptanRecord> id = StringIdFor.convert(TramStations.Bury.getId(), NaptanRecord.class);
        IdFor<NPTGLocality > localityId = NPTGLocality.createId("nptgId1234");
        String commonName = "theCommonName";
        GridPosition gridPosition= KnownLocations.atMancArena.getGridPosition();
        LatLong latlong = KnownLocations.atMancArena.latLong();
        String suburb = "inSurburbia";
        String town = "Manchester";
        NaptanStopType stopType = NaptanStopType.airportEntrance;
        String street = "Coronation Street";
        String indicator = "indicatorZ";
        boolean localityCenter = false;

        IdFor<Station> railStationId = Station.InvalidId();
        NaptanRecord original = new NaptanRecord(id, localityId, commonName, gridPosition, latlong, suburb, town,
                stopType, street, indicator, localityCenter, railStationId);

        String text = objectMapper.writeValueAsString(original);

        NaptanRecord result = objectMapper.readValue(text, NaptanRecord.class);

        assertEquals(id, result.getId());
        assertEquals(stopType, result.getStopType());

        assertEquals(railStationId, result.getRailStationId());

    }
}
