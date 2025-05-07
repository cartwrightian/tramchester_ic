package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Replacement Bus
    ReplacementBus1(BusOne, "Replacement Bus Rochdale - Freehold", "2462", startMayCutover),

    // Blue
    EcclesAshtonNew(Blue, "Eccles - Manchester - Ashton Under Lyne", "2119", startMayCutover),
    //EcclesAshton(Blue, "Eccles - Manchester - Ashton Under Lyne", "2871", endAprilCutover),

    // Green
    BuryManchesterAltrinchamNew(Green, "Bury - Manchester - Altrincham", "841", startMayCutover),
    //BuryManchesterAltrincham(Green, "Bury - Manchester - Altrincham", "2872", endAprilCutover),

    // Navy
    VictoriaManchesterAirportNew(Navy, "Victoria - Wythenshawe - Manchester Airport", "2120", startMayCutover),
    //VictoriaManchesterAirport(Navy, "Victoria - Wythenshawe - Manchester Airport", "2873", endAprilCutover),

    // Pink
    RochdaleManchesterEastDidisburyNew(Pink, "Rochdale - Manchester - East Didsbury", "845", startMayCutover),
    //RochdaleManchesterEastDidisbury(Pink, "Rochdale - Manchester - East Didsbury", "2874", endAprilCutover),

    EtihadPiccadillyAltrinchamNew(Purple, "Etihad Campus - Piccadilly - Altrincham", "2173", startMayCutover),
    //EtihadPiccadillyAltrincham(Purple, "Etihad Campus - Piccadilly - Altrincham", "2875", endAprilCutover),

    // Red
    CornbrookTheTraffordCentreNew(Red, "Etihad Campus - The Trafford Centre", "849", startMayCutover),
    //CornbrookTheTraffordCentre(Red, "Deansgate-Castlefield - The Trafford Centre", "2876", endAprilCutover),

    // Yellow, not in tfgm data as of 28/3/2025
    AshtonCrumpsall(Yellow, "Ashton - Crumpsall Bay", "844", startMayCutover);
    //PiccadillyBury(Yellow, "Piccadilly - Bury", "2877", endAprilCutover);

    private final TFGMRouteNames line;
    private final String longName; // diagnostics only
    private final TramRouteId id;
    private final TramDate validFrom;

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom) {
        this.longName = longName;
        this.id = TramRouteId.create(line, id);
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

    public TFGMRouteNames line() {
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

    @Override
    public String toString() {
        return line + "["+name()+"]";
    }
}
