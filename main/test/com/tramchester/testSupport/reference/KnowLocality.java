package com.tramchester.testSupport.reference;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;

import java.util.EnumSet;

public enum KnowLocality {
    Altrincham("E0028261"),
    Macclesfield("N0076467"),
    Shudehill("N0077806"),
    ManchesterAirport("N0075057"),
    Stockport("E0057819"),
    Knutsford("E0044368"),
    Broadheath("E0028613"),
    OldfieldBrow("E0029171"),

    // out of bounds
    LondonWestminster("E0034961");

    private final String localityId;

    public static final EnumSet<KnowLocality> GreaterManchester = EnumSet.range(KnowLocality.Altrincham, Knutsford);

    KnowLocality(String localityId) {
        this.localityId = localityId;
    }

    public IdFor<NPTGLocality> getId() {
        return NPTGLocality.createId(localityId);
    }
}
