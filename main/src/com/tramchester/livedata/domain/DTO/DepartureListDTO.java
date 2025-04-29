package com.tramchester.livedata.domain.DTO;

import com.tramchester.domain.presentation.Note;

import java.util.List;
import java.util.SortedSet;

public class DepartureListDTO {

    private SortedSet<DepartureDTO> departures;
    private List<Note> notes;
    private boolean forJourney;

    public DepartureListDTO() {
        // for deserialisation
    }

    public DepartureListDTO(SortedSet<DepartureDTO> departures, List<Note> notes, boolean forJourney) {
        this.departures = departures;
        this.notes = notes;
        this.forJourney = forJourney;
    }

    public SortedSet<DepartureDTO> getDepartures() {
        return departures;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public boolean isForJourney() {
        return forJourney;
    }

    @Override
    public String toString() {
        return "DepartureListDTO{" +
                "departures=" + departures +
                ", notes=" + notes +
                ", forJourney=" + forJourney +
                '}';
    }
}
