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
import static com.tramchester.testSupport.reference.KnownTramRoute.latestCutoverDate;
import static com.tramchester.testSupport.reference.KnownTramRoute.newRouteIdsDate;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Replacement Buses

    ReplacementBusOne(BusOne, "Replacement Bus One", "3214", TramDate.of(2026,3,22)),
    ReplacementBusOneNew(BusOne, "Replacement Bus One", "3224", newRouteIdsDate),
    //ReplacementBusTwo(BusTwo, "Replacement Bus Two", "2736", EmergencyWorksDeansgateDev2025),
    //ReplacementBusThree(BusThree, "Replacement Bus Three", "3082", TramDate.of(2025,11,1)),

    // Blue
    EcclesAshtonNew(Blue, "Eccles - Manchester - Ashton Under Lyne", "3207", latestCutoverDate),
    EcclesAshtonNewB(Blue, "Eccles - Manchester - Ashton Under Lyne", "3217", newRouteIdsDate),

    // Green
    BuryManchesterAltrincham(Green, "Bury - Manchester - Altrincham", "3208", latestCutoverDate),
    BuryManchesterAltrinchamB(Green, "Bury - Manchester - Altrincham", "3218", newRouteIdsDate.plusDays(1)),

    // Navy
    VictoriaManchesterAirport(Navy, "Victoria - Wythenshawe - Manchester Airport", "3209", latestCutoverDate),
    VictoriaManchesterAirportB(Navy, "Victoria - Wythenshawe - Manchester Airport", "3219", newRouteIdsDate),

    // Pink
    RochdaleManchesterEastDidisbury(Pink, "Rochdale - Manchester - East Didsbury", "3210", latestCutoverDate),
    RochdaleManchesterEastDidisburyB(Pink, "Rochdale - Manchester - East Didsbury", "3220", newRouteIdsDate),

    // Purple
    EtihadPiccadillyAltrincham(Purple, "Etihad Campus - Piccadilly - Altrincham", "3211", latestCutoverDate),
    EtihadPiccadillyAltrinchamB(Purple, "Etihad Campus - Piccadilly - Altrincham", "3221", newRouteIdsDate),

    // Red
    CornbrookTheTraffordCentre(Red, "Etihad Campus - The Trafford Centre", "3212", latestCutoverDate),
    CornbrookTheTraffordCentreB(Red, "Etihad Campus - The Trafford Centre", "3222", newRouteIdsDate),

    // Yellow
    AshtonCrumpsall(Yellow, "Ashton - Crumpsall Bay", "3213", latestCutoverDate),
    AshtonCrumpsallB(Yellow, "Ashton - Crumpsall Bay", "3223", newRouteIdsDate);


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
