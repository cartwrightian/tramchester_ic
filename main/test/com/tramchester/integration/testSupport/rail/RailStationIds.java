package com.tramchester.integration.testSupport.rail;

import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;


public enum RailStationIds implements HasId<Station> {
    Stockport(createId("STKP"), true, "SPT"),
    ManchesterPiccadilly(createId("MNCRPIC"), true, "MAN"),
    ManchesterVictoria(createId("MNCRVIC"), true, "MCV"),
    ManchesterDeansgate(createId("MNCRDGT"), true, "DGT"),
    ManchesterOxfordRoad(createId("MNCROXR"), true, "MCO"),
    ManchesterAirport(createId("MNCRIAP"), true, "MIA"),
    SalfordCentral(createId("SLFDORD"), true, "SFD"),
    Altrincham(createId("ALTRNHM"), true, "ALT"),
    NavigationRaod(createId("NAVGTNR"), true, "NVR"),
    Crewe(createId("CREWE"), false, "CRE"),
    LondonEuston(createId("EUSTON"), false, "EUS"),
    Derby(createId("DRBY"), false, "DBY"),
    Belper(createId("BELPER"), false, "BLP"),
    Duffield(createId("DUFIELD"), false, "DFI"),
    Dover(createId("DOVERP"), false, "DVP"),
    Wimbledon(createId("WDON"), false, "WIM"),
    LondonWaterloo(createId("WATRLMN"), false, "WAT"),
    LondonStPancras(createId("STPX"), false, "STP"),
    Macclesfield(createId("MACLSFD"), false, "MAC"),
    MiltonKeynesCentral(createId("MKNSCEN"), false, "MKC"),
    Hale(createId("HALE"), true, "HAL"),
    Knutsford(createId("KNUTSFD"), false, "KNF"),
    Ashley(createId("ASHLEY"), true, "ASY"),
    Mobberley(createId("MOBERLY"), true, "MOB"),
    StokeOnTrent(createId("STOKEOT"), false, "SOT"),
    Delamere(createId("DELAMER"), false, "DLM"),
    Wilmslow(createId("WLMSL"), true, "WML"),
    Chester(createId("CHST"), false, "CTR"),
    EastDidsbury(createId("EDIDBRY"), true, "EDY"),
    Eccles(createId("ECCLES"), true, "ECC"),
    Inverness(createId("IVRNESS"), false, "INV"),
    LiverpoolLimeStreet(createId("LVRPLSH"), false, "LIV"),
    Huddersfield(createId("HDRSFLD"), false, "HUD"),
    Ashton(createId("ASHONUL"), true, "AHN"),
    Levenshulme(createId("LVHM"), true, "LVM"),
    Leeds(createId("LEEDS"), false, "LDS"),
    Saltburn(createId("SBRN"), false, "SLB");

    private static IdFor<Station> createId(String text) {
        return Station.createId(text);
    }

    private final IdFor<Station> id;
    private final String crs;
    private final boolean isGreaterManchester;

    RailStationIds(IdFor<Station> id, boolean isGreaterManchester, String crs) {
        this.id = id;
        this.isGreaterManchester = isGreaterManchester;
        this.crs = crs;
    }

    public IdFor<Station> getId() {
        return id;
    }

    public Station from(final StationRepository repository) {
        return repository.getStationById(getId());
    }

    public IdForDTO getIdDTO() {
        return new IdForDTO(id);
    }

    public boolean isGreaterManchester() {
        return isGreaterManchester;
    }

    public String crs() {
        return crs;
    }

    public Station from(final CRSRepository crsRepository) {
        return crsRepository.getFor(crs);
    }
}
