package com.tramchester.unit.domain.id;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.reference.TFGMRouteNames;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TramRouteIdTest {

    @Test
    void shouldHaveEquals() {
        TramRouteId routeIdA = TramRouteId.create(TFGMRouteNames.Green, "998877");
        TramRouteId routeIdB = TramRouteId.create(TFGMRouteNames.Green, "998877");
        TramRouteId routeIdC = TramRouteId.create(TFGMRouteNames.Red, "998877");
        TramRouteId routeIdD = TramRouteId.create(TFGMRouteNames.Green, "1223");

        assertEquals(routeIdA, routeIdB);
        assertEquals(routeIdB, routeIdA);
        assertNotEquals(routeIdA, routeIdC);
        assertNotEquals(routeIdA, routeIdD);

    }

    @Test
    void shouldHaveEqualityWithValidStringRouteId() {
        IdFor<Route> idA  = TramRouteId.create(TFGMRouteNames.Green, "998877");

        IdFor<Route> idB = Route.createBasicRouteId("Green>>998877");

        assertEquals(idA, idB);
        assertEquals(idB, idA);

        // need same hash otherwise issue with collections with mixed sources
        assertEquals(idA.hashCode(), idB.hashCode());
    }

    @Test
    void shouldRoundtripSerilisation() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        TramRouteId id = TramRouteId.create(TFGMRouteNames.Green, "998877");

        String text = mapper.writeValueAsString(id);

        TramRouteId result = mapper.readValue(text, TramRouteId.class);

        assertEquals(id, result);
    }

    @Test
    void shouldRoundTripGraphId() {
        TramRouteId id = TramRouteId.create(TFGMRouteNames.Green, "998877");

        String text = id.getGraphId();

        IdFor<Route> result = Route.parse(text);

        assertEquals(id, result);
    }


}
