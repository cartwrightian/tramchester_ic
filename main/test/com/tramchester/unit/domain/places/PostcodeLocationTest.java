package com.tramchester.unit.domain.places;

import com.tramchester.domain.id.PostcodeLocationId;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.geo.GridPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PostcodeLocationTest {

    @Test
    void shouldGetAreaAndName() {
        GridPosition gridPosition = GridPosition.Invalid;

        PostcodeLocationId idA = PostcodeLocation.createId("M17AB");
        PostcodeLocation locationA = new PostcodeLocation(gridPosition, idA);
        Assertions.assertEquals("M17AB", locationA.getName());
        Assertions.assertEquals(idA, locationA.getId());

        PostcodeLocationId idB = PostcodeLocation.createId("wa114ab");
        PostcodeLocation locationB = new PostcodeLocation(gridPosition, idB);
        Assertions.assertEquals("WA114AB", locationB.getName());
        Assertions.assertEquals(idB, locationB.getId());

        PostcodeLocationId idC = PostcodeLocation.createId("B114AB");
        PostcodeLocation locationC = new PostcodeLocation(gridPosition, idC);
        Assertions.assertEquals("B114AB", locationC.getName());
        Assertions.assertEquals(idC, locationC.getId());

    }
}
