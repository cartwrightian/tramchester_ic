package com.tramchester.testSupport.reference;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;

public enum KnowLocality {
    Altrincham("E0028261"),
    LondonWestminster("E0034961"),
    Macclesfield("N0076467"),
    Shudehill("N0077806"),
    ManchesterAirport("N0075057"),
    Stockport("E0057819"),
    Knutsford("E0044368");

    private final String localityId;

    KnowLocality(String localityId) {
        this.localityId = localityId;
    }

    public IdFor<NPTGLocality> getId() {
        return NPTGLocality.createId(localityId);
    }
}
