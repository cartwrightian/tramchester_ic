package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLines.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.MISSING_ROUTE;
import static com.tramchester.testSupport.reference.KnownTramRoute.marchCutoverC;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Blue
    EcclesAshton(Blue, "Eccles - Manchester - Ashton Under Lyne", "2119", marchCutoverC),

    // Green
    BuryManchesterAltrincham(Green, "Bury - Manchester - Altrincham", "841", marchCutoverC),

    // Navy
    DeansgateCastlefieldManchesterAirport(Navy, "Deansgate-Castlefield - Manchester Airport", "2120", marchCutoverC),

    // Pink
    RochdaleShawandCromptonManchesterEastDidisbury(Pink, "Rochdale - Manchester - East Didsbury", "2831", marchCutoverC),
    RochdaleShawandCromptonManchesterEastDidisburyNewC(Pink, "Rochdale - Manchester - East Didsbury", "845", TramDate.of(2025, 4,6)),
    RochdaleShawandCromptonManchesterEastDidisburyNewD(Pink, "Rochdale - Manchester - East Didsbury", "2831", TramDate.of(2025, 4,7)),

    // Purple, not in tfgm data as of 28/3/2025
    EtihadPiccadillyAltrinchamNew(Purple, "Etihad Campus - Piccadilly - Altrincham", MISSING_ROUTE, marchCutoverC),

    // Red
    CornbrookTheTraffordCentre(Red, "Cornbrook - The Trafford Centre", "849", marchCutoverC),

    // Yellow, not in tfgm data as of 28/3/2025
    PiccadillyVictoriaNewInvalid(Yellow, "Piccadilly - Victoria", MISSING_ROUTE, marchCutoverC);

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

    public static EnumSet<KnownTramRouteEnum> validRoutes() {
        return Arrays.stream(values()).filter(item -> item.getId().isValid()).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(KnownTramRouteEnum.class)));
    }

    public TramDate getValidFrom() {
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
