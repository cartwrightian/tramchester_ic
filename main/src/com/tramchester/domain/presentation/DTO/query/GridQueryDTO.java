package com.tramchester.domain.presentation.DTO.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@JsonTypeName("GridQuery")
public class GridQueryDTO {

    final private LocationType destType;
    final private IdForDTO destId;
    final private LocalDate departureDate;
    final private LocalTime departureTime;
    final private long maxDuration; // todo should be an int
    final private int maxChanges;
    final private int gridSize;

    @JsonCreator
    public GridQueryDTO(
            @JsonProperty(value="destType", required = true) LocationType destType,
            @JsonProperty(value="destId", required = true) IdForDTO destId,
            @JsonProperty(value="departureDate", required = true) LocalDate departureDate,
            @JsonProperty(value="departureTime", required = true) LocalTime departureTime,
            @JsonProperty(value="maxDuration", required = true) long maxDuration,
            @JsonProperty(value="maxChanges", required = true) int maxChanges,
            @JsonProperty(value="gridSize", required = true) int gridSize) {
        this.destType = destType;
        this.destId = destId;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.maxDuration = maxDuration;
        this.maxChanges = maxChanges;
        this.gridSize = gridSize;
    }

    @JsonIgnore
    public TramDate getTramDate() {
        return TramDate.of(departureDate);
    }

    // needed for serialization
    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public long getMaxDuration() {
        return maxDuration;
    }

    @JsonIgnore
    public TramTime getDepartureTramTime() {
        return TramTime.ofHourMins(departureTime);
    }

    // needed for serialization
    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public int getMaxChanges() {
        return maxChanges;
    }

    public int getGridSize() {
        return gridSize;
    }

    public LocationType getDestType() {
        return destType;
    }

    public IdForDTO getDestId() {
        return destId;
    }

    @Override
    public String toString() {
        return "GridQueryDTO{" +
                "destType=" + destType +
                ", destId=" + destId +
                ", departureDate=" + departureDate +
                ", departureTime=" + departureTime +
                ", maxDuration=" + maxDuration +
                ", maxChanges=" + maxChanges +
                ", gridSize=" + gridSize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridQueryDTO that = (GridQueryDTO) o;
        return maxDuration == that.maxDuration && maxChanges == that.maxChanges && gridSize == that.gridSize
                && destType == that.destType && Objects.equals(destId, that.destId)
                && Objects.equals(departureDate, that.departureDate) && Objects.equals(departureTime, that.departureTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destType, destId, departureDate, departureTime, maxDuration, maxChanges, gridSize);
    }
}
