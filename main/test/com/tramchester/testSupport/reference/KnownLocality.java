package com.tramchester.testSupport.reference;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.integration.graph.buses.BusRouteToRouteCostsTest;
import com.tramchester.repository.StationGroupsRepository;

import java.util.EnumSet;

public enum KnownLocality {
    Altrincham("E0028261"),
    Macclesfield("N0076467"),
    Shudehill("N0077806"),
    ManchesterAirport("N0075057"),
    Stockport("E0057819"),
    Knutsford("E0044368"),
    Broadheath("E0028613"),
    ManchesterCityCentre("E0057786"),
    Manchester("N0076111"),
    PiccadillyGardens("N0075071"),
    Bollington("E0044356"),

    // see below, keep last of the GM locations
    OldfieldBrow("E0029171"),

    // out of bounds
    LondonWestminster("E0034961");

    private final String localityId;

    public static final EnumSet<KnownLocality> GreaterManchester = EnumSet.range(KnownLocality.Altrincham, Knutsford);

    KnownLocality(String localityId) {
        this.localityId = localityId;
    }

    public IdFor<NPTGLocality> getAreaId() {
        return NPTGLocality.createId(localityId);
    }

    public IdFor<StationGroup> getId() {
        return StationGroup.createId(localityId);
    }

    public IdFor<NPTGLocality> getLocalityId() {
        return NPTGLocality.createId(localityId);
    }

    public StationGroup from(StationGroupsRepository stationGroupRepository) {
        return stationGroupRepository.getStationGroup(getId());
    }

    /***
     * @see BusRouteToRouteCostsTest for verification, useful to record this as used on several tests
     */
    public static final int MIN_CHANGES = 2;
}
