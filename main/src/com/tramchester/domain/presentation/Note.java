package com.tramchester.domain.presentation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;

import java.util.List;
import java.util.Objects;


@SuppressWarnings("unused")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = StationNote.class)})
public class Note {
    private NoteType noteType;
    private String text;

    public Note(String text, NoteType noteType) {
        this.text = text;
        this.noteType = noteType;
    }

    public Note() {
        // deserialisation
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public String getText() {
        return text;
    }

    public List<LocationRefDTO> getDisplayedAt() {
        return null;
    }

    public enum NoteType {
        Weekend,
        Christmas,
        ClosedStation,
        Live,
        Diversion
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return noteType == note.noteType &&
                Objects.equals(text, note.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteType, text);
    }

    @Override
    public String toString() {
        return "Note{" +
                "noteType=" + noteType +
                ", text='" + text + '\'' +
                '}';
    }
}
