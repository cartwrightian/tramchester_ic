package com.tramchester.domain.presentation;

import com.tramchester.domain.presentation.DTO.LocationRefDTO;

import java.util.List;

public class StationNote extends Note {

    private List<LocationRefDTO> displayedAt;

    public StationNote() {
        // deserialisation
        super();
    }

    public StationNote(NoteType noteType, String text, List<LocationRefDTO> displayedAt) {
        super(text, noteType);
        this.displayedAt = displayedAt;
    }

    public List<LocationRefDTO>  getDisplayedAt() {
        return displayedAt;
    }

    @Override
    public String toString() {
        return "StationNote{" +
                "station=" + displayedAt +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StationNote that = (StationNote) o;

        return getDisplayedAt() != null ? getDisplayedAt().equals(that.getDisplayedAt()) : that.getDisplayedAt() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getDisplayedAt() != null ? getDisplayedAt().hashCode() : 0);
        return result;
    }
}
