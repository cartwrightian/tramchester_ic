package com.tramchester.testSupport;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;

import java.util.Arrays;

import static com.tramchester.testSupport.reference.TramStations.*;

public class AdditionalTramInterchanges {

    public static ImmutableIdSet<Station> stations() {
        return IdSet.from(Arrays.asList(Firswood.getId(), Chorlton.getId()));
    }

}
