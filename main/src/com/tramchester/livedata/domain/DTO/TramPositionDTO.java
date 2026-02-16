package com.tramchester.livedata.domain.DTO;

import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.time.TramDuration;

import java.util.Set;

@SuppressWarnings("unused")
public class TramPositionDTO {
    private LocationRefWithPosition first;
    private LocationRefWithPosition second;
    private Set<DepartureDTO> trams;
    private int cost;

    public TramPositionDTO() {
        // deserialisation
    }

    public TramPositionDTO(LocationRefWithPosition first, LocationRefWithPosition second, Set<DepartureDTO> trams, TramDuration cost) {
        this.first = first;
        this.second = second;
        this.trams = trams;
        this.cost = (int) cost.toMinutes();
    }

    public LocationRefWithPosition getFirst() {
        return first;
    }

    public LocationRefWithPosition getSecond() {
        return second;
    }

    public Set<DepartureDTO> getTrams() {
        return trams;
    }

    public int getCost() {
        return cost;
    }
}
