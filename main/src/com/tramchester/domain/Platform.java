package com.tramchester.domain;

import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

public interface Platform extends GraphProperty, CoreDomain, Location<Platform> {

    String getPlatformNumber();

    /***
     * Use PlatformId.createId()
     * @param station the station for the platform
     * @param platformNumber the number of the platform
     * @return the Id
     */
    @Deprecated
    static PlatformId createId(Station station, String platformNumber) {
        return PlatformId.createId(station, platformNumber);
    }

    Station getStation();

}
