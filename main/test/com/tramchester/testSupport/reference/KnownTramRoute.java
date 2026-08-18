package com.tramchester.testSupport.reference;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.dates.TramDate;
import org.jetbrains.annotations.NotNull;

public class KnownTramRoute {

    public static @NotNull KnownTramRouteEnum getPink(TramDate date) {
        return KnownTramRouteEnum.PinkRoute;
    }

    public static @NotNull KnownTramRouteEnum getNavy(TramDate date) {
        return KnownTramRouteEnum.NavyRoute;
    }

    public static @NotNull KnownTramRouteEnum getBlue(TramDate date) {
        return KnownTramRouteEnum.BlueRoute;
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }

    public static MutableRoute getRed() {
        return KnownTramRouteEnum.RedRoute.fake();
    }

    public static MutableRoute getPink() {
        return KnownTramRouteEnum.PinkRoute.fake();
    }

    public static MutableRoute getBlue() {
        return KnownTramRouteEnum.BlueRoute.fake();
    }

    public static MutableRoute getNavy() {
        return KnownTramRouteEnum.NavyRoute.fake();
    }


}
