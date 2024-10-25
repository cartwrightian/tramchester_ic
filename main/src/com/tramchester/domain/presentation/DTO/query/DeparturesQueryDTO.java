package com.tramchester.domain.presentation.DTO.query;

import com.fasterxml.jackson.annotation.*;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;

import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@JsonTypeName("DeparturesQuery")
public class DeparturesQueryDTO {

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("locationType")
    private LocationType locationType;

    @JsonProperty("locationId")
    private IdForDTO locationId;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("finalStationId")
    private IdForDTO finalStationId;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("modes")
    private Set<TransportMode> modes;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("notesFor")
    private Set<IdForDTO> notesFor;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("firstDestIds")
    private Set<IdForDTO> firstDestIds;

    public DeparturesQueryDTO(LocationType locationType, IdForDTO locationId) {
        this.locationType = locationType;
        this.locationId = locationId;
        modes = Collections.emptySet();
    }

    public DeparturesQueryDTO() {
        modes = Collections.emptySet();
        time = LocalTime.MAX;
        // deserialisation
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public IdForDTO getLocationId() {
        return locationId;
    }

    public EnumSet<TransportMode> getModes() {
        if (modes.isEmpty()) {
            return EnumSet.noneOf(TransportMode.class);
        }
        return EnumSet.copyOf(modes);
    }

    public void setModes(Set<TransportMode> modes) {
        this.modes = modes;
    }

    public boolean hasValidTime() {
        return time != LocalTime.MAX;
    }

    public void setNotesFor(Set<IdForDTO> notesFor) {
        this.notesFor = notesFor;
    }

    public Set<IdForDTO> getNotesFor() {
        return notesFor;
    }

    public Set<IdForDTO> getFirstDestIds() {
        return firstDestIds;
    }

    public void setFirstDestIds(Set<IdForDTO> firstDestIds) {
        this.firstDestIds = firstDestIds;
    }

    @Override
    public String toString() {
        return "DeparturesQueryDTO{" +
                "time=" + time +
                ", locationType=" + locationType +
                ", locationId=" + locationId +
                ", finalStationId=" + finalStationId +
                ", modes=" + modes +
                ", notesFor=" + notesFor +
                ", firstDestId=" + firstDestIds +
                '}';
    }

    @JsonIgnore
    public boolean hasDestinationsIds() {
        if (firstDestIds==null) {
            return false;
        }
        return finalStationId != null;
//        return !firstDestIds.isEmpty();
    }

    public IdForDTO getFinalStationId() {
        return finalStationId;
    }

    public void setFinalStationId(IdForDTO finalStationId) {
        this.finalStationId = finalStationId;
    }
}
