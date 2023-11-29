package com.tramchester.domain;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

public interface Platform extends GraphProperty, CoreDomain, Location<Platform> {

    String getPlatformNumber();

    Station getStation();

}
