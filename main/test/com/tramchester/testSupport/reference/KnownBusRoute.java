package com.tramchester.testSupport.reference;

import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;

public enum KnownBusRoute {
    AltrinchamMacclesfield("Altrincham - Wilmslow - Knutsford - Macclesfield", "7778511"),
    MacclesfieldAirport("Macclesfield - Manchester Airport", "7778511");

    private final String name;
    private final IdFor<Agency> agencyId;

    KnownBusRoute(String name, String agencyIdText) {
        this.name = name;
        this.agencyId = Agency.createId(agencyIdText);
    }

    public String getName() {
        return name;
    }

    public IdFor<Agency> getAgencyId() {
        return agencyId;
    }
}
