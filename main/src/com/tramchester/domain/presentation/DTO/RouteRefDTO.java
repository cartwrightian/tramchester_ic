package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;

@JsonIgnoreProperties(value = "tram", allowGetters = true)
public class RouteRefDTO implements HasIdForDTO {

    private IdForDTO id;
    private String routeName;
    private TransportMode transportMode;
    private String shortName;

    @SuppressWarnings("unused")
    public RouteRefDTO() {
        // deserialization
    }

    public RouteRefDTO(Route route) {
        this(route, route.getName());
    }

    public RouteRefDTO(Route route, String routeName) {
        this.routeName = routeName;

        // tfgm data have routes that are identical except for the ID, don't want to expose this to the API
        //this.id = route.getId().forDTO();

        this.transportMode = route.getTransportMode();
        this.shortName = route.getShortName();
        this.id = IdForDTO.createFor(route);
    }

    public String getRouteName() {
        return routeName;
    }

    public String getShortName() {
        return shortName;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    /***
     * Use getId
     * @return route id
     */
    @Deprecated
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public IdForDTO getRouteID() {
        return id;
    }

    @Override
    public IdForDTO getId() {
        return id;
    }

    // use TransportMode
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Boolean isTram() {
        return transportMode.equals(TransportMode.Tram);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteRefDTO that = (RouteRefDTO) o;

        return routeName.equals(that.routeName);
    }

    @Override
    public int hashCode() {
        return routeName.hashCode();
    }

    @Override
    public String toString() {
        return "RouteRefDTO{" +
                "id='" + id + '\'' +
                ", routeName='" + routeName + '\'' +
                ", transportMode=" + transportMode +
                ", shortName='" + shortName + '\'' +
                '}';
    }

}
