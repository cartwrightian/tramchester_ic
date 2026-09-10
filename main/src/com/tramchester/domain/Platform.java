package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

public interface Platform extends GraphProperty, CoreDomain, Location<Platform>, HasGraphLabel {

    static IdFor<Platform> InvalidId() {
        return new InvalidId<>(Platform.class);
    }

    String getPlatformNumber();

    Station getStation();

}
