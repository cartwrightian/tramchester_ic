package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import static com.tramchester.testSupport.reference.KnownLines.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.marchCutover;
import static com.tramchester.testSupport.reference.KnownTramRoute.revertDate;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
enum KnownTramRouteEnum implements TestRoute {


    // Blue
    EcclesAshton(Blue, "Eccles - Manchester - Ashton Under Lyne", "2119", revertDate),
    EcclesAshtonNew(Blue, "Eccles - Manchester - Ashton Under Lyne", "2778", marchCutover),
    // Green
    BuryManchesterAltrincham(Green, "Bury - Manchester - Altrincham", "841", revertDate),
    BuryManchesterAltrinchamNew(Green, "Bury - Manchester - Altrincham", "2779", marchCutover),
    // Navy
    DeansgateCastlefieldManchesterAirport(Navy, "Deansgate-Castlefield - Manchester Airport", "2120", revertDate),
    DeansgateCastlefieldManchesterAirportNew(Navy, "Deansgate-Castlefield - Manchester Airport", "2780", marchCutover),
    // Pink
    RochdaleShawandCromptonManchesterEastDidisbury(Pink, "Rochdale - Manchester - East Didsbury", "845", revertDate),
    RochdaleShawandCromptonManchesterEastDidisburyNew(Pink, "Rochdale - Manchester - East Didsbury", "2781", marchCutover),
    // Purple
    EtihadPiccadillyAltrincham(Purple, "Etihad Campus - Piccadilly - Altrincham", "2173", revertDate),
    // Red
    CornbrookTheTraffordCentre(Red, "Cornbrook - The Trafford Centre", "849", revertDate),
    CornbrookTheTraffordCentreNew(Red, "Cornbrook - The Trafford Centre", "2782", marchCutover),
    // Yellow
    PiccadillyVictoria(Yellow, "Piccadilly - Victoria", "844", revertDate),
    PiccadillyVictoriaNew(Yellow, "Piccadilly - Victoria", "2783", marchCutover);

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
