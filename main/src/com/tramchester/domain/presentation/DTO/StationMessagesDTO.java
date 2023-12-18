package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.Note;

import java.util.List;

public class StationMessagesDTO {
    private List<Note> notes;

    public StationMessagesDTO() {
        // deserialization
    }

    public StationMessagesDTO(List<Note> notes) {
        this.notes = notes;
    }

    public List<Note> getNotes() {
        return notes;
    }
}
