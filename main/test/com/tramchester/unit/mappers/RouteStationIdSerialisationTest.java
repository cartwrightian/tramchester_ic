package com.tramchester.unit.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouteStationIdSerialisationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void onceBeforeEachTest() {
        mapper = new ObjectMapper();
    }

    @Test
    void shouldRoundTripRouteId() throws JsonProcessingException {
        IdFor<Route> routeId = Route.createId("routeA");
        IdFor<Station> stationId = Station.createId("stationB");
        RouteStationId id = RouteStationId.createId(routeId, stationId);

        String asString = mapper.writeValueAsString(id);

        RouteStationId result = mapper.readValue(asString, RouteStationId.class);

        assertEquals(routeId, result.getRouteId());
        assertEquals(stationId, result.getStationId());

    }


}
