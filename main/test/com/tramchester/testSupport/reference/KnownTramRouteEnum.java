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
import static com.tramchester.testSupport.reference.KnownTramRoute.newRouteIdsDate;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Replacement Buses
//    ReplacementBusOne(BusOne, "Replacement Bus 1", "3080", ReplacementBusEaster2026),
//    ReplacementBusTwo(BusTwo, "Replacement Bus 2", "2736", ReplacementBusEaster2026),
//    ReplacementBusThree(BusThree, "Replacement Bus 3", "2361", ReplacementBusEaster2026),
//    ReplacementBusFour(BusFour, "Replacement Bus 4", "3229", ReplacementBusEaster2026),
//    ReplacementBusFive(BusFive, "Replacement Bus 5", "2177", ReplacementBusEaster2026),
    ReplacementBusBlue(BusBlue,"Replacement Bus Blue", "3224", TramDate.of(2026,4,11)),

    // Blue
    EcclesAshton(Blue, "Eccles - Manchester - Ashton Under Lyne", "3217", newRouteIdsDate),

    // Green
    BuryManchesterAltrincham(Green, "Bury - Manchester - Altrincham", "3218", newRouteIdsDate),

    // Navy
    VictoriaManchesterAirport(Navy, "Victoria - Wythenshawe - Manchester Airport", "3219", newRouteIdsDate),

    // Pink
    RochdaleManchesterEastDidisbury(Pink, "Rochdale - Manchester - East Didsbury", "3220", newRouteIdsDate),

    // Purple
    EtihadPiccadillyAltrincham(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", newRouteIdsDate),

    // Red
    CornbrookTheTraffordCentre(Red, "Etihad Campus - The Trafford Centre", "3222", newRouteIdsDate),

    // Yellow
    AshtonCrumpsall(Yellow, "Ashton - Crumpsall Bay", "3223", newRouteIdsDate);


    private final TFGMRouteNames line;
    private final String longName; // diagnostics only
    private final IdFor<Route> id;
    private final TramDate validFrom;

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id, TramDate validFrom) {
        this.longName = longName;
        if (id.isEmpty()) {
            this.id = IdFor.invalid(Route.class);
        } else {
            this.id = TramRouteId.create(line, id);
        }
        this.validFrom = validFrom;
        this.line = line;
    }

    public static EnumSet<KnownTramRouteEnum> validRoutes() {
        return Arrays.stream(values()).
                filter(item -> item.getId().isValid()).
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
