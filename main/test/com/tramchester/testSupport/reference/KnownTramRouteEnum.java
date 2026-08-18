package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;

import static com.tramchester.domain.reference.TFGMRouteNames.*;

/*
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRouteEnum implements TestRoute {

    // Blue
    BlueRoute(Blue, "Eccles - Ashton-under-Lyne", "1111"),
    GreenRoute(Green, "Altrincham - Bury", "2222"),
    NavyRoute(Navy, "Manchester Airport - Victoria", "3333"),
    PinkRoute(Pink, "East Didsbury - Rochdale" , "4444"),
    Purple2(Purple, "Altrincham - Etihad Campus", "5555"),
    RedRoute(Red, "Trafford Centre - Crumpsall", "6666"),
    YellowRoute(Yellow, "Piccadilly - Bury", "7777"),

    ;

    private final TFGMRouteNames line;
    private final String longName;
    private final String id;

    KnownTramRouteEnum(TFGMRouteNames line, String longName, String id) {
        this.longName = longName;
        this.line = line;
        this.id = id;
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
        if (id.isEmpty()) {
           return IdFor.invalid(Route.class);
        } else {
            return TramRouteId.create(line, id);
        }
    }

    @Override
    public IdForDTO dtoId() {
        return IdForDTO.createFor(getId());
    }

    @Override
    public MutableRoute fake() {
        return new MutableRoute(getId(), line.getShortName(), longName, TestEnv.MetAgency(), TransportMode.Tram);
    }

    @Override
    public String toString() {
        return line + "["+name()+"]";
    }

}
