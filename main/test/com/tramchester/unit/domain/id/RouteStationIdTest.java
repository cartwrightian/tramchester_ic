package com.tramchester.unit.domain.id;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.Test;

import static com.tramchester.integration.testSupport.rail.RailStationIds.LondonEuston;
import static com.tramchester.integration.testSupport.rail.RailStationIds.StokeOnTrent;
import static org.junit.jupiter.api.Assertions.*;

public class RouteStationIdTest {

    private final IdFor<Station> stationId = Station.createId("1234");
    private final IdFor<Route> routeA = Route.createBasicRouteId("routeA");

    @Test
    void shouldHaveMixedCompositeEquality() {
        IdFor<Route> routeB = Route.createBasicRouteId("routeB");

        IdFor<RouteStation> compositeIdA = RouteStationId.createId(routeA, stationId);
        IdFor<RouteStation> compositeIdB = RouteStationId.createId(routeA, stationId);
        IdFor<RouteStation> compositeIdC = RouteStationId.createId(routeB, stationId);

        assertEquals(compositeIdA, compositeIdA);
        assertEquals(compositeIdA, compositeIdB);
        assertEquals(compositeIdB, compositeIdA);

        assertNotEquals(compositeIdA, compositeIdC);
        assertNotEquals(compositeIdC, compositeIdA);
    }

    @Test
    void shouldOutputGraphIdAsExpected() {

        IdFor<RouteStation> compositeIdA = RouteStationId.createId(routeA, stationId);

        assertEquals("routeA_1234", compositeIdA.getGraphId());
    }

    @Test
    void shouldOutputParseGraphIdAsExpected() {

        IdFor<RouteStation> expected = RouteStationId.createId(routeA, stationId);

        IdFor<RouteStation> id = RouteStationId.parse("routeA_1234");
        assertEquals(id, expected);
    }

    @Test
    void shouldRoundTripParseMixedComposite() {
        IdFor<RouteStation> compositeIdA = RouteStationId.createId(routeA, stationId);

        String forDto = compositeIdA.getGraphId();
        IdFor<RouteStation> result = RouteStationId.parse(forDto);

        assertEquals(compositeIdA, result);
    }

    @Test
    void shouldRoundTripRouteStationIdBasic() throws JsonProcessingException {
        validateRoundTrip(Route.createBasicRouteId("routeA"));
    }

    @Test
    void shouldRoundTripRouteStationIdTramRoute() throws JsonProcessingException {
        validateRoundTrip(KnownTramRoute.getGreen(TestEnv.testDay()).getId());
    }

    @Test
    void shouldRoundTripRouteStationIdRailRoute() throws JsonProcessingException {
        IdFor<Route> id  = new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), Agency.createId("NT"), 1);
        validateRoundTrip(id);
    }


    private static void validateRoundTrip(IdFor<Route> routeId) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        IdFor<Station> stationId = Station.createId("stationB");
        RouteStationId id = RouteStationId.createId(routeId, stationId);

        String asString = mapper.writeValueAsString(id);

        RouteStationId result = mapper.readValue(asString, RouteStationId.class);

        assertEquals(routeId, result.getRouteId());
        assertEquals(stationId, result.getStationId());
    }


}
