package com.tramchester.unit.domain.id;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.graph.GraphPropertyKey;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphProperyKeyTest {

    @Test
    void shouldHaveAKeyForEachDomainTypeWithAnId() {
        Reflections reflections = new Reflections(CoreDomain.class.getPackageName());

        final Set<Class<? extends CoreDomain>> allDomain = reflections.getSubTypesOf(CoreDomain.class);

        Set<Class<? extends CoreDomain>> hasId = allDomain.stream().
                filter(klass -> !klass.isInterface()).
                filter(HasId.class::isAssignableFrom).
                filter(GraphProperty.class::isAssignableFrom).
                collect(Collectors.toSet());

        // Not in graph - need to sort this out which is due to the way Location is currently implemented
        hasId.remove(PostcodeLocation.class);

        assertFalse(hasId.isEmpty());

        hasId.forEach(klass -> {
            GraphPropertyKey key = GraphPropertyKey.getFor(klass);
            assertTrue(key.isDomainId(), "Not marked as an id " + key);
        });
    }

    @Test
    void shouldBeAbleToParseIds() {
        List<GraphPropertyKey> idKeys = Arrays.stream(GraphPropertyKey.values()).
                filter(GraphPropertyKey::isDomainId).toList();

        idKeys.forEach(key -> {
            String text = textFor(key);
            IdFor<?> id = IdFor.parse(key, text);
            assertTrue(id.isValid(), "not valid for " + text + " key " + key);
        });
    }

    private String textFor(GraphPropertyKey key) {
        return switch (key) {
            case WALK_ID -> "53.485846, -2.239472";
            case ROUTE_STATION_ID -> "routeA_1234";
            default -> "someText";
        };
    }
}
