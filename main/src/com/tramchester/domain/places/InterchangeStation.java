package com.tramchester.domain.places;

import com.tramchester.domain.HasRoutes;
import com.tramchester.domain.HasTransportModes;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public interface InterchangeStation extends HasId<Station>, HasTransportModes, HasRoutes {
    boolean isMultiMode();

    InterchangeType getType();

    Station getStation();

    default IdFor<Station> getStationId() {
        return getId();
    }

    LocationId<?> getLocationId();

    boolean isAvailableSameMode(ImmutableEnumSet<TransportMode> modes);

    /***
     * For simple interchange will be the single station, for linked interchanges this is main station plus
     * all linked stations
     * @return set of all stations associated with this interchange
     */
    Set<Station> getAllStations();
}
