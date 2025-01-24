package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;

import java.util.List;


/***
 * Note: serializable for RouteIndex cache purposes
 */
public class RailRouteId extends ContainsId<Route> implements IdFor<Route> {

    private final IdFor<Station> begin;
    private final IdFor<Station> end;
    private final IdFor<Agency> agencyId;
    private final int index;
    private final StringIdFor<Route> containedId;

    @JsonCreator
    public RailRouteId(@JsonProperty("begin") IdFor<Station> begin,
                       @JsonProperty("end") IdFor<Station> end,
                       @JsonProperty("agencyId") IdFor<Agency> agencyId,
                       @JsonProperty("index") int index) {

        containedId = createContainedId(begin, end, agencyId, index);
        this.begin = begin;
        this.end = end;
        this.agencyId = agencyId;
        this.index = index;
    }

    public static RailRouteId createId(final IdFor<Agency> agencyId, final List<IdFor<Station>> callingPoints, final int index) {
        if (callingPoints.size()<2) {
            throw new RuntimeException("Need at least two calling points to create rail route id, got " + callingPoints);
        }
        final IdFor<Station> first = callingPoints.getFirst();
        final IdFor<Station> last = callingPoints.getLast();
        return new RailRouteId(first, last, agencyId, index);
    }

    private static StringIdFor<Route> createContainedId(final IdFor<Station> begin, final IdFor<Station> end,
                                                        final IdFor<Agency> agency, final int index) {
        final StringIdFor<Station> beginId = (StringIdFor<Station>) begin;
        final StringIdFor<Station> endId = (StringIdFor<Station>) end;
        final StringIdFor<Agency> agencyId = (StringIdFor<Agency>) agency;

        final String idText = String.format("%s:%s=>%s:%s", beginId.getContainedId(), endId.getContainedId(),
                agencyId.getContainedId(), index);

        return new StringIdFor<>(idText, Route.class);
    }

    @JsonIgnore
    @Override
    public String getGraphId() {
        return containedId.getGraphId();
    }

    @JsonIgnore
    @Override
    public boolean isValid() {
        return true;
    }

    // so this ends up in the json, for diagnostic reasons
    @JsonProperty("diagnostics")
    @Override
    StringIdFor<Route> getContainedId() {
        return containedId;
    }

    @JsonIgnore
    @Override
    public Class<Route> getDomainType() {
        return Route.class;
    }

    public IdFor<Station> getBegin() {
        return begin;
    }

    public IdFor<Station> getEnd() {
        return end;
    }

    public IdFor<Agency> getAgencyId() {
        return agencyId;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) {
            return false;
        }
        if (StringIdFor.class == o.getClass()) {
            StringIdFor<?> other = (StringIdFor<?>) o;
            if (other.getDomainType() != Route.class) {
                return false;
            }
            return containedId.getContainedId().equals(other.getContainedId());
        }
        //if (o == null || getClass() != o.getClass()) return false;
        RailRouteId that = (RailRouteId) o;
        return containedId.equals(that.containedId);
    }

    @Override
    public int hashCode() {
        return containedId.hashCode();
    }

    @Override
    public String toString() {
        return "RailRouteId{" +
//                "begin=" + begin +
//                ", end=" + end +
//                ", agencyId=" + agencyId +
//                ", index=" + index +
                " containedId=" + containedId +
                "} ";
    }
}
