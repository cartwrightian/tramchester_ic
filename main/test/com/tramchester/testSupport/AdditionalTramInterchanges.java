package com.tramchester.testSupport;

import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.reference.FakeStation;

import static com.tramchester.testSupport.reference.TramStations.*;

public class AdditionalTramInterchanges {

    // NOTE: rebuild Graph after changing this

    public static ImmutableIdSet<Station> stations() {
        return FakeStation.IdSetOf(MediaCityUK, Piccadilly, Etihad);
    }

}
