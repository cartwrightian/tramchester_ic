package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;

import java.util.List;

public class RouteDTO extends RouteRefDTO {

    private List<LocationRefWithPosition> stations;

    public RouteDTO(Route route, List<LocationRefWithPosition> stations) {
        super(route);
        this.stations = stations;
    }

    @SuppressWarnings("unused")
    public RouteDTO() {
        // deserialisation
    }

    public List<LocationRefWithPosition> getStations() {
        return stations;
    }

    @Override
    public String toString() {
        return "RouteDTO{" +
                "stations=" + stations +
                "} " + super.toString();
    }

    public IdForDTO getStartStation() {
        return stations.get(0).getId();
    }
}
