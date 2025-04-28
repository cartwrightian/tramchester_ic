package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

import java.util.Set;

public interface Location<TYPE extends Location<TYPE>> extends HasGridPosition, HasTransportModes,
        GraphProperty, HasGraphLabel, CoreDomain , HasId<TYPE>, HasRoutes {

    String getName();

    LatLong getLatLong();

    GridPosition getGridPosition();

    IdFor<NPTGLocality> getLocalityId();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    LocationType getLocationType();

    DataSourceID getDataSourceID();

    boolean hasPickup();

    boolean hasDropoff();

    // can be used as a part of a journey, see also closed stations which are temporary changes
    boolean isActive();

    // marked as an interchange in the source data
    boolean isMarkedInterchange();

    LocationId<TYPE> getLocationId();

    boolean containsOthers();

    LocationSet<Station> getAllContained();
}
