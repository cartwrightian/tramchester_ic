package com.tramchester.integration.testSupport.rail;

import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.reference.FakeStation;


public enum RailStationIds implements HasId<Station>, FakeStation {
    Stockport("STKP", true, "SPT"),
    ManchesterPiccadilly("MNCRPIC", true, "MAN"),
    ManchesterVictoria("MNCRVIC", true, "MCV"),
    ManchesterDeansgate("MNCRDGT", true, "DGT"),
    ManchesterOxfordRoad("MNCROXR", true, "MCO"),
    ManchesterAirport("MNCRIAP", true, "MIA"),
    SalfordCentral("SLFDORD", true, "SFD"),
    Altrincham("ALTRNHM", true, "ALT"),
    NavigationRaod("NAVGTNR", true, "NVR"),
    Crewe("CREWE", false, "CRE"),
    LondonEuston("EUSTON", false, "EUS"),
    Derby("DRBY", false, "DBY"),
    Belper("BELPER", false, "BLP"),
    Duffield("DUFIELD", false, "DFI"),
    Dover("DOVERP", false, "DVP"),
    Wimbledon("WDON", false, "WIM"),
    LondonWaterloo("WATRLMN", false, "WAT"),
    LondonStPancras("STPX", false, "STP"),
    Macclesfield("MACLSFD", false, "MAC"),
    MiltonKeynesCentral("MKNSCEN", false, "MKC"),
    Hale("HALE", true, "HAL"),
    Knutsford("KNUTSFD", false, "KNF"),
    Ashley("ASHLEY", true, "ASY"),
    Mobberley("MOBERLY", true, "MOB"),
    StokeOnTrent("STOKEOT", false, "SOT"),
    Delamere("DELAMER", false, "DLM"),
    Wilmslow("WLMSL", true, "WML"),
    Chester("CHST", false, "CTR"),
    EastDidsbury("EDIDBRY", true, "EDY"),
    Eccles("ECCLES", true, "ECC"),
    Inverness("IVRNESS", false, "INV"),
    LiverpoolLimeStreet("LVRPLSH", false, "LIV"),
    Huddersfield("HDRSFLD", false, "HUD"),
    Ashton("ASHONUL", true, "AHN"),
    Levenshulme("LVHM", true, "LVM"),
    Leeds("LEEDS", false, "LDS"),
    Saltburn("SBRN", false, "SLB");

    private final String rawId;
    private final String crs;
    private final boolean isGreaterManchester;

    RailStationIds(String rawId, boolean isGreaterManchester, String crs) {
        this.rawId = rawId;
        this.isGreaterManchester = isGreaterManchester;
        this.crs = crs;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public LatLong getLatLong() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public String getRawId() {
        return rawId;
    }

    @Override
    public Station fake() {
        throw new RuntimeException("Not implemented yet");
    }

    public boolean isGreaterManchester() {
        return isGreaterManchester;
    }

    public String crs() {
        return crs;
    }

    public Station from(final CRSRepository crsRepository) {
        return crsRepository.getStationFor(crs);
    }
}
