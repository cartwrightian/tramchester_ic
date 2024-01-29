package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;

public class StationIdPair extends LocationIdPair<Station> {
    public StationIdPair(Station begin, Station end) {
        super(begin, end);
    }

    public StationIdPair(IdFor<Station> begin, IdFor<Station> end) {
        super(begin, end, LocationType.Station);
    }

    public static StationIdPair of(HasId<Station> begin, HasId<Station> end) {
        return new StationIdPair(begin.getId(), end.getId());
    }

    public static StationIdPair of(IdFor<Station> begin, IdFor<Station> end) {
        return new StationIdPair(begin, end);
    }


}
