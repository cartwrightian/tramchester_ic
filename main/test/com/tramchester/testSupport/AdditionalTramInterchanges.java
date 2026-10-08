package com.tramchester.testSupport;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;

public class AdditionalTramInterchanges {

    // NOTE: rebuild Graph after changing this

    public static ImmutableIdSet<Station> stations() {
        return IdSet.emptySet();
        //return FakeStation.IdSetOf(MediaCityUK, Piccadilly, Etihad);
    }

}
