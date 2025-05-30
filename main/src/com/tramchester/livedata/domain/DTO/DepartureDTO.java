package com.tramchester.livedata.domain.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@SuppressWarnings("unused")
@JsonPropertyOrder(alphabetic = true)
public class DepartureDTO implements Comparable<DepartureDTO> {

    // NOTE: was used for live data archive but those S3 buckets were, erm lost, when IC moved jobs

    // TODO Make 'from' a StationRefDTO?
    private TransportMode transportMode;
    private LocationRefDTO from;
    private IdForDTO destinationId;
    private String destinationName;
    private String carriages;
    private String status;
    private LocalDateTime dueTime;
    private boolean matchesJourney;

    @JsonIgnore
    private LocalDateTime lastUpdated;

    public DepartureDTO() {
        // for deserialisation
    }

    public DepartureDTO(Location<?> start, UpcomingDeparture upcomingDeparture, LocalDateTime updateTime, boolean matchesJourney) {
        this(upcomingDeparture.getMode(),
                new LocationRefDTO(start),
                IdForDTO.createFor(upcomingDeparture.getDestinationId()),
                upcomingDeparture.getDestinationName(),
                upcomingDeparture.getCarriages(), upcomingDeparture.getStatus(),
                upcomingDeparture.getWhen().toDate(updateTime.toLocalDate()), updateTime, matchesJourney);
    }

    private DepartureDTO(TransportMode mode, LocationRefDTO from, IdForDTO destinationId, String destinationName, String carriages, String status, LocalDateTime dueTime,
                         LocalDateTime lastUpdated, boolean matchesJourney) {
        if (destinationName==null) {
            throw new RuntimeException("Cannot have null name, id was " + destinationId);
        }
        this.transportMode = mode;
        this.from = from;
        this.destinationName = destinationName;
        this.destinationId = destinationId;
        this.carriages = carriages;
        this.status = status;
        this.dueTime = dueTime;
        this.matchesJourney = matchesJourney;
        this.lastUpdated = lastUpdated;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateTimeFormatForJson)
    public LocalDateTime getDueTime() {
        return dueTime;
    }

    public LocationRefDTO getFrom() {
        return from;
    }

    public String getCarriages() {
        return carriages;
    }

    public String getStatus() {
        return status;
    }

    public IdForDTO getDestinationId() {
        return destinationId;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public boolean getMatchesJourney() {
        return matchesJourney;
    }

    public String getDestinationName() {
        return destinationName;
    }

    @Override
    public int compareTo(DepartureDTO other) {
        if (dueTime.equals(other.dueTime)) {
            // if same time use name ordering
            return destinationName.compareTo(other.destinationName);
        }
        // time ordering
        return dueTime.compareTo(other.dueTime);
    }

    @Override
    public String toString() {
        return "DepartureDTO{" +
                "transportMode=" + transportMode +
                ", from='" + from + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", destinationId='" + destinationId + '\'' +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", dueTime=" + dueTime +
                ", matchesJourney=" + matchesJourney +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartureDTO that = (DepartureDTO) o;
        return transportMode == that.transportMode && from.equals(that.from) && destinationName.equals(that.destinationName)
                && destinationId.equals(that.destinationId)
                && carriages.equals(that.carriages) && status.equals(that.status)
                && (matchesJourney==that.matchesJourney)
                && dueTime.equals(that.dueTime) && lastUpdated.equals(that.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transportMode, from, destinationName, destinationId, carriages, status, dueTime, lastUpdated, matchesJourney);
    }

    @JsonProperty(value = "wait", access = JsonProperty.Access.READ_ONLY)
    public int getWait() {
        Duration duration = Duration.between(lastUpdated.truncatedTo(ChronoUnit.MINUTES), dueTime);
        long minutes = duration.toMinutes();

        if (minutes<0) {
            return 0;
        }

        return (int) minutes;
    }

}
