package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public class StationToStationConnectionDTO {
    private LocationRefWithPosition begin;
    private LocationRefWithPosition end;
    private Set<TransportMode> transportModes;
    private Double distanceInMeters;

    public StationToStationConnectionDTO(LocationRefWithPosition begin, LocationRefWithPosition end, Set<TransportMode> transportModes,
                                         Double distanceInMeters) {
        this.begin = begin;
        this.end = end;
        this.transportModes = transportModes;
        this.distanceInMeters = distanceInMeters;
    }

    public StationToStationConnectionDTO() {
        // deserialisation
    }

    public LocationRefWithPosition getBegin() {
        return begin;
    }

    public LocationRefWithPosition getEnd() {
        return end;
    }

    public Set<TransportMode> getTransportModes() {
        return transportModes;
    }

    public Double getDistanceInMeters() {
        return distanceInMeters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationToStationConnectionDTO that = (StationToStationConnectionDTO) o;

        if (!begin.equals(that.begin)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = begin.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

}
