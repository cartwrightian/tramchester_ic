package com.tramchester.unit.domain.id;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StringIdForTest {
    @Test
    void shouldFormIdAsExpected() {
        IdFor<Station> id = getStationId("stationId");

        assertEquals("stationId", id.getGraphId());
        assertEquals(Station.class, id.getDomainType());
        assertTrue(id.isValid());

    }

    @Test
    void shouldRemoveIdFromString() {
        IdFor<Station> id = getStationId("stationId");

        String result = StringIdFor.removeIdFrom("stationId42xx", id);
        assertEquals(result,"42xx");
    }

    @Test
    void shouldConcatWithId() {
        IdFor<Station> id = getStationId("stationId");

        IdFor<Platform> expected = StringIdFor.createId("stationIdpostfix", Platform.class);

        StringIdFor<Platform> result = StringIdFor.concat(id, "postfix", Platform.class);

        assertEquals(Platform.class, result.getDomainType());
        assertEquals(expected, result);
    }

    @Test
    void shouldTestConvert() {
        IdFor<Station> id = getStationId("stationId");
        IdFor<Route> expected = StringIdFor.createId("stationId", Route.class);


        IdFor<Route> result = StringIdFor.convert(id, Route.class);

        assertEquals(expected, result);
        assertEquals(Route.class, result.getDomainType());
    }

    @Test
    void shouldTestInvalid() {
        IdFor<Station> id = StringIdFor.invalid(Station.class);

        assertEquals(Station.class, id.getDomainType());
        assertFalse(id.isValid());
    }

    @Test
    void shouldTestPrefix() {
        IdFor<Station> id = getStationId("stationId");
        IdFor<Route> expected = StringIdFor.createId("prefixstationId", Route.class);

        IdFor<Route> prefixed = StringIdFor.withPrefix("prefix", id, Route.class);

        assertEquals(expected, prefixed);
    }

    @Test
    void shouldTestSetOf() {

        Set<String> items = new HashSet<>(Arrays.asList("itemA", "itemB", "itemC"));
        IdSet<Station> ids = StringIdFor.createIds(items, Station.class);

        assertEquals(3, ids.size());
        assertTrue(ids.contains(getStationId("itemA")));
        assertTrue(ids.contains(getStationId("itemB")));
        assertTrue(ids.contains(getStationId("itemC")));

    }


    @NotNull
    private static IdFor<Station> getStationId(String stationId) {
        return StringIdFor.createId(stationId, Station.class);
    }

}
