package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.GTFSTransportationType;

import java.util.Objects;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteData {

    @JsonProperty("route_id")
    private String id;
    @JsonProperty("route_short_name")
    private String shortName;
    @JsonProperty("route_long_name")
    private String longName;
    @JsonProperty("agency_id")
    private String agencyid;
    @JsonProperty("route_type")
    private String routeType;

    public RouteData () {
        // for deserialisation
    }

    private String removeSpaces(String text) {
        return text.replaceAll(" ", "");
    }

//    public IdFor<Route> getId() {
//        return Route.createId(removeSpaces(id));
//    }

    public String getId() {
        return removeSpaces(id);
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName.trim();
    }

    public IdFor<Agency> getAgencyId() {
        return Agency.createId(agencyid);
    }

    public GTFSTransportationType getRouteType() {
        return GTFSTransportationType.parse(routeType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteData routeData = (RouteData) o;
        return Objects.equals(id, routeData.id);
    }

    @Override
    public String toString() {
        return "RouteData{" +
                "id='" + id + '\'' +
                ", shortName='" + shortName + '\'' +
                ", longName='" + longName + '\'' +
                ", agencyid='" + agencyid + '\'' +
                ", routeType=" + getRouteType() +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
