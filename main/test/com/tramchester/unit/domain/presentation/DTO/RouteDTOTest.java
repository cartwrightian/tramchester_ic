package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.TraffordCentre;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteDTOTest {

    @Test
    void shouldUseRouteNameForEquality() {

        List<LocationRefWithPosition> stations = new ArrayList<>();

        stations.add(new LocationRefWithPosition(TraffordCentre.fake()));
        MutableRoute red = KnownTramRoute.getRed();
        RouteDTO routeDTO = new RouteDTO(red, stations);

        // no longer used at FE for Tram at least
        assertEquals("Trafford Centre - Crumpsall", routeDTO.getRouteName());
        assertEquals(red.getShortName(), routeDTO.getShortName());
        assertEquals(TransportMode.Tram, routeDTO.getTransportMode());
        assertEquals(new IdForDTO(red.getId()), routeDTO.getId());

        List<LocationRefWithPosition> stationsDTO = routeDTO.getStations();
        assertEquals(1, stationsDTO.size());
        assertEquals(TraffordCentre.getIdForDTO(), stations.getFirst().getId());
    }

}
