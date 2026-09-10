package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphPropertyKey;
import org.jetbrains.annotations.NotNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public interface IdFor<T extends CoreDomain> extends Comparable<IdFor<T>> {

    static <T extends CoreDomain> IdFor<T> invalid(Class<T> domainType) {
        return new InvalidId<>(domainType);
    }

    String getGraphId();

    boolean isValid();

    Class<T> getDomainType();

    @Override
    default int compareTo(@NotNull IdFor<T> o) {
        return this.getGraphId().compareTo(o.getGraphId());
    }

    static IdFor<?> parse(final GraphPropertyKey key, final String text) {
        if (key==GraphPropertyKey.ROUTE_STATION_ID) {
            return RouteStationId.parse(text);
        }
        return switch (key) {
            case STATION_ID -> Station.createId(text);
            case SERVICE_ID -> Service.createId(text);
            case PLATFORM_ID -> StringIdFor.createId(text, Platform.class);
            case TRIP_ID -> Trip.createId(text);
            case ROUTE_ID -> StringIdFor.createId(text, Route.class);
            case AREA_ID -> StringIdFor.createId(text, NPTGLocality.class);
            case STATION_GROUP_ID -> StringIdFor.createId(text, StationGroup.class);
            case WALK_ID -> MyLocation.parseFromId(text).getId();
            default -> throw new RuntimeException("Cannot parse for " + key + " and " + text);
        };
    }


}
