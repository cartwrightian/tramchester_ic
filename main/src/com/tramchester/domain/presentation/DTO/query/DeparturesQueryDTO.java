package com.tramchester.domain.presentation.DTO.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.Nulls;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.reference.TransportMode;

import java.time.LocalTime;
import java.util.*;

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
    @JsonProperty("modes")
    private Set<TransportMode> modes;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("notesFor")
    private Set<IdForDTO> notesFor;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    @JsonProperty("journeys")
    private List<JourneyDTO> journeys;

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

    @Override
    public String toString() {
        return "DeparturesQueryDTO{" +
                "time=" + time +
                ", locationType=" + locationType +
                ", locationId=" + locationId +
                ", finalStationId=" + journeys +
                ", modes=" + modes +
                ", notesFor=" + notesFor +
                '}';
    }

    public boolean hasJourneys() {
        return !journeys.isEmpty();
    }

    public List<JourneyDTO> getJourneys() {
        return journeys;
    }

    public void setJourneys(Collection<JourneyDTO> journeys) {
        this.journeys = new ArrayList<>(journeys);
    }
}
