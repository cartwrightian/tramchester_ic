package com.tramchester.domain.places;

import com.tramchester.domain.HasRoutes;
import com.tramchester.domain.HasTransportModes;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;

public interface InterchangeStation extends HasId<Station>, HasTransportModes, HasRoutes {
    boolean isMultiMode();

    InterchangeType getType();

    Station getStation();

    default IdFor<Station> getStationId() {
        return getId();
    }

}
