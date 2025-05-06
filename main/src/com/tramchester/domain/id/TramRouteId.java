package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.TFGMRouteNames;

public class TramRouteId extends ContainsId<Route> {
    public static final String DIVIDER = ">>";

    private final TFGMRouteNames routeName;
    private final String idText;

    public TramRouteId(@JsonProperty("routeName") final TFGMRouteNames routeName,
                       @JsonProperty("idText") final String idText) {

        super(StringIdFor.createId(routeName.name() + DIVIDER + idText, Route.class));
        this.routeName = routeName;
        this.idText = idText;
    }

    // serialisation
    @JsonProperty("routeName")
    public TFGMRouteNames getRouteName() {
        return routeName;
    }

    // serialisation
    @JsonProperty("idText")
    public String getIdText() {
        return idText;
    }

    public static TramRouteId create(final TFGMRouteNames routeName, final String text) {
        return new TramRouteId(routeName, text);
    }

    public static boolean matches(final String text) {
        return text.contains(DIVIDER);
    }

    public static IdFor<Route> parse(final String text) {
        final int indexOf = text.indexOf(DIVIDER);
        if (indexOf<0) {
            throw new RuntimeException("Missing '" +DIVIDER + "' in " + text);
        }
        final TFGMRouteNames routeName = TFGMRouteNames.parseFromName(text.substring(0, indexOf));
        final String idText = text.substring(indexOf+DIVIDER.length());
        return create(routeName, idText);
    }

    @JsonIgnore
    @Override
    public boolean isValid() {
        return getContainedId().isValid();
    }

    @Override
    public String toString() {
        return "TramRouteId{" + getContainedId().getContainedId() + '}';
    }
}
