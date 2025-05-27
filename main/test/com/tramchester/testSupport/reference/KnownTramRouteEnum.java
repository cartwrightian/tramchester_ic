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
    ReplacementBus1EndMay(BusOne, "Replacement Bus One", "2462", TramDate.of(2025,5,30)),

    // Blue
    EcclesAshtonNew(Blue, "Eccles - Manchester - Ashton Under Lyne", "2119", startMayCutover),

    // Green
    BuryManchesterAltrinchamNew(Green, "Bury - Manchester - Altrincham", "841", startMayCutover),

    // Navy
    VictoriaManchesterAirportNew(Navy, "Victoria - Wythenshawe - Manchester Airport", "2120", startMayCutover),

    // Pink
    RochdaleManchesterEastDidisburyNew(Pink, "Rochdale - Manchester - East Didsbury", "845", startMayCutover),

    EtihadPiccadillyAltrinchamNew(Purple, "Etihad Campus - Piccadilly - Altrincham", "2173", startMayCutover),

    // Red
    CornbrookTheTraffordCentreNew(Red, "Etihad Campus - The Trafford Centre", "849", startMayCutover),

    // Yellow
    AshtonCrumpsall(Yellow, "Ashton - Crumpsall Bay", "844", startMayCutover);

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
