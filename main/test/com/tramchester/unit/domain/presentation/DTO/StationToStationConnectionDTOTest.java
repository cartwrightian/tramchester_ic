package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationToStationConnectionDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.reference.TransportMode;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.EnumSet;

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationToStationConnectionDTOTest extends EasyMockSupport {

    private DTOFactory stationDTOFactory;

    @BeforeEach
    void beforeEachTestRuns() {
        stationDTOFactory = new DTOFactory();
    }

    @Test
    void shouldCreateTramLink() {
        final Station altrincham = Altrincham.fake();
        final Station stPeters = StPetersSquare.fake();

        EnumSet<TransportMode> modes = EnumSet.of(TransportMode.Bus, TransportMode.Tram);

        Quantity<Length> distance = Quantities.getQuantity(42.5768D, Units.METRE);
        StationToStationConnection stationLink = new StationToStationConnection(altrincham, stPeters, modes, distance, Duration.ofSeconds(124));

        replayAll();
        StationToStationConnectionDTO dto = stationDTOFactory.createStationLinkDTO(stationLink);
        verifyAll();

        assertEquals(IdForDTO.createFor(altrincham), dto.getBegin().getId());
        assertEquals(IdForDTO.createFor(stPeters), dto.getEnd().getId());

        assertEquals(2, dto.getTransportModes().size());

        assertTrue( dto.getTransportModes().contains(TransportMode.Bus));
        assertTrue( dto.getTransportModes().contains(TransportMode.Tram));

        assertEquals(distance.getValue().doubleValue(), dto.getDistanceInMeters());

    }
}
