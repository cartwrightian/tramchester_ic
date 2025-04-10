package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TestRoute;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.TraffordCentre;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteDTOTest {

    private static TramDate when = TestEnv.testDay();
    private final static TestRoute knownRoute = KnownTramRoute.getRed(when);


    @Test
    void shouldUseRouteNameForEquality() {

        List<LocationRefWithPosition> stations = new ArrayList<>();

        stations.add(new LocationRefWithPosition(TraffordCentre.fake()));
        RouteDTO routeDTO = new RouteDTO(getRoute(), stations);

        // no longer used at FE for Tram at least
        assertEquals("CornbrookTheTraffordCentre", routeDTO.getRouteName());
        assertEquals(knownRoute.shortName(), routeDTO.getShortName());
        assertEquals(TransportMode.Tram, routeDTO.getTransportMode());

        List<LocationRefWithPosition> stationsDTO = routeDTO.getStations();
        assertEquals(1, stationsDTO.size());
        assertEquals(TraffordCentre.getIdForDTO(), stations.getFirst().getId());
    }

    public Route getRoute() {
        return MutableRoute.getRoute(knownRoute.getId(), knownRoute.shortName(), "CornbrookTheTraffordCentre", TestEnv.MetAgency(),
                knownRoute.mode());
    }
}
