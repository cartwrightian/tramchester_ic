package com.tramchester.domain.places;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.TransportMode;

import java.time.Duration;
import java.util.Set;

public interface Station extends Location<Station> {

    Set<Platform> getPlatformsForRoute(Route route);

    boolean hasPlatform(IdFor<Platform> platformId);

    // is marked as a central location in the naptan data
    boolean isCentral();

//    /***
//     * @param route route to check
//     * @return true if station serves given route, use with care ignores the date
//     */
//    boolean servesRoutePickup(Route route);
//
//    /***
//     * @param route route to check
//     * @return true if station serves given route, use with care ignores the date
//     */
//    boolean servesRouteDropOff(Route route);

    boolean servesMode(TransportMode mode);

    Duration getMinChangeDuration();

    static IdFor<Station> createId(String text) {
        return StringIdFor.createId(text, Station.class);
    }

    static IdFor<Station> createId(IdForDTO idForDTO) {
        return StringIdFor.createId(idForDTO, Station.class);
    }

    static IdFor<Station> createId(IdFor<NaptanRecord> naptanId) {
        return StringIdFor.convert(naptanId, Station.class);
    }

    static IdFor<Station> InvalidId() {
        return StringIdFor.invalid(Station.class);
    }

}
