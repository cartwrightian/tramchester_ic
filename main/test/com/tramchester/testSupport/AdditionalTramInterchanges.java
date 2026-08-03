package com.tramchester.testSupport;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;

public class AdditionalTramInterchanges {

    public static ImmutableIdSet<Station> stations() {
        return IdSet.emptySet();
        //return IdSet.from(Arrays.asList(Firswood.getId(), Chorlton.getId()));
    }

}
