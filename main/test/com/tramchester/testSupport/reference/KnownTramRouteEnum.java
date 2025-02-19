package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import static com.tramchester.testSupport.reference.KnownLines.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.febCutOver;
import static com.tramchester.testSupport.reference.KnownTramRoute.revertDate;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
enum KnownTramRouteEnum implements TestRoute {

    // Replacement buses Eccles
    //BusEcclesToMediaCity(BusOne, "Eccles - Media City", "2749", revertDate),
    BusEcclesToMediaCity_new(BusOne, "Eccles - Media City", "2757", febCutOver),

    BusPiccVictoria(BusTwo, "Replacement Bus Piccadilly - Victoria", "2461",
            TramDate.of(2025,2,23)),

    // Blue
    EcclesAshton(Blue, "Eccles - Manchester - Ashton Under Lyne", "2119", revertDate),
    EcclesAshton_new(Blue, "Eccles - Manchester - Ashton Under Lyne", "2750", febCutOver),

    // Green
    BuryManchesterAltrincham(Green, "Bury - Manchester - Altrincham", "841", revertDate),
    BuryManchesterAltrincham_new(Green, "Bury - Manchester - Altrincham", "2751", febCutOver),

    // Navy
    DeansgateCastlefieldManchesterAirport(Navy, "Deansgate-Castlefield - Manchester Airport", "2120", revertDate),
    DeansgateCastlefieldManchesterAirport_new(Navy, "Deansgate-Castlefield - Manchester Airport", "2752", febCutOver),

    // Pink
    RochdaleShawandCromptonManchesterEastDidisbury(Pink, "Rochdale - Manchester - East Didsbury", "845", revertDate),
    RochdaleShawandCromptonManchesterEastDidisbury_new(Pink, "Rochdale - Manchester - East Didsbury", "2753", febCutOver),

    // Purple
    EtihadPiccadillyAltrincham(Purple, "Etihad Campus - Piccadilly - Altrincham", "2173", revertDate),
    EtihadPiccadillyAltrincham_new(Purple, "Etihad Campus - Piccadilly - Altrincham", "2754", febCutOver),

    // Red
    CornbrookTheTraffordCentre(Red, "Cornbrook - The Trafford Centre", "849", revertDate),
    CornbrookTheTraffordCentr_new(Red, "Cornbrook - The Trafford Centre", "2755", febCutOver),


    // Yellow
    PiccadillyVictoria(Yellow, "Piccadilly - Victoria", "844", revertDate),
    PiccadillyVictoria_new(Yellow, "Piccadilly - Victoria", "2756", febCutOver);

    //private final String shortName;
    private final KnownLines line;
    private final String longName; // diagnostics only
    private final IdFor<Route> id;
    private final TramDate validFrom;

    KnownTramRouteEnum(KnownLines line, String longName, String id, TramDate validFrom) {
        this.longName = longName;
        this.id = Route.createId(id);
        this.validFrom = validFrom;
        this.line = line;
    }

    TramDate getValidFrom() {
        return validFrom;
    }

    @Override
    public TransportMode mode() {
        return TransportMode.Tram;
    }

    /**
     * @return short name for a route
     */
    @Override
    public String shortName() {
        return line.getShortName();
    }

    @Override
    public IdFor<Route> getId() {
        return id;
    }

    @Override
    public IdForDTO dtoId() {
        return IdForDTO.createFor(id);
    }

    @Override
    public Route fake() {
        return new MutableRoute(id, line.getShortName(), longName, TestEnv.MetAgency(), TransportMode.Tram);
    }
}
