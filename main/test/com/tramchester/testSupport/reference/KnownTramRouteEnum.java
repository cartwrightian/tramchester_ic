package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import static com.tramchester.testSupport.UpcomingDates.DeansgateTraffordBarWorks;
import static com.tramchester.testSupport.reference.KnownLines.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
enum KnownTramRouteEnum implements TestRoute {

    // Blue
    EcclesAshton(Blue, "Eccles - Manchester - Ashton Under Lyne", "2119", revertDate),
    EcclesAshtonNewA(Blue, "Eccles - Manchester - Ashton Under Lyne", "2778", marchCutoverA),
    EcclesAshtonNewB(Blue, "Eccles - Manchester - Ashton Under Lyne", "2801", marchCutoverB),
    // Green
    BuryManchesterAltrincham(Green, "Bury - Manchester - Altrincham", "841", revertDate),
    BuryManchesterAltrinchamNewA(Green, "Bury - Manchester - Altrincham", "2779", marchCutoverA),
    BuryManchesterAltrinchamNewB(Green, "Bury - Manchester - Altrincham", "2802", marchCutoverB),
    // Navy
    DeansgateCastlefieldManchesterAirport(Navy, "Deansgate-Castlefield - Manchester Airport", "2120", revertDate),
    DeansgateCastlefieldManchesterAirportNewA(Navy, "Deansgate-Castlefield - Manchester Airport", "2780", marchCutoverA),
    DeansgateCastlefieldManchesterAirportNewB(Navy, "Deansgate-Castlefield - Manchester Airport", "2803", marchCutoverB),
    // Pink
    RochdaleShawandCromptonManchesterEastDidisbury(Pink, "Rochdale - Manchester - East Didsbury", "845", revertDate),
    RochdaleShawandCromptonManchesterEastDidisburyNewA(Pink, "Rochdale - Manchester - East Didsbury", "2781", marchCutoverA),
    RochdaleShawandCromptonManchesterEastDidisburyNewB(Pink, "Rochdale - Manchester - East Didsbury", "2804", marchCutoverB),

    // Purple
    EtihadPiccadillyAltrincham(Purple, "Etihad Campus - Piccadilly - Altrincham", "2173", revertDate),
    EtihadPiccadillyAltrinchamNew(Purple, "Etihad Campus - Piccadilly - Altrincham", "2806", marchCutoverB),

    // Red
    CornbrookTheTraffordCentre(Red, "Cornbrook - The Trafford Centre", "849", revertDate),
    CornbrookTheTraffordCentreNewA(Red, "Cornbrook - The Trafford Centre", "2782", marchCutoverA),
    CornbrookTheTraffordCentreNewB(Red, "Cornbrook - The Trafford Centre", "2807", marchCutoverB),

    // Brown 2811,7778482,Brown Line,Firswood - East Didsbury,,0,,,
    FirswoodEastDidsbury(Brown, "Firswood - East Didsbury", "2811", DeansgateTraffordBarWorks.getStartDate()),


    // Yellow
    PiccadillyVictoria(Yellow, "Piccadilly - Victoria", "844", revertDate),
    PiccadillyVictoriaNewA(Yellow, "Piccadilly - Victoria", "2783", marchCutoverA),
    PiccadillyVictoriaNewB(Yellow, "Piccadilly - Victoria", "2808", marchCutoverB);
    ;

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

    public KnownLines line() {
        return line;
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
