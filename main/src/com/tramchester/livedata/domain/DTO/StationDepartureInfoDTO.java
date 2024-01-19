package com.tramchester.livedata.domain.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class StationDepartureInfoDTO  {
    private String lineName;
    private IdForDTO stationPlatform;
    private String message;
    private List<DepartureDTO> dueTrams;
    private LocalDateTime lastUpdate;
    private String displayId;
    private String location;

    ///
    /// NOTE: used for archiving of received live data into json on S3
    ///
    public StationDepartureInfoDTO(String lineName, IdForDTO stationPlatform, String message, List<DepartureDTO> dueTrams,
                                   LocalDateTime lastUpdate, String displayId, String location) {
        this.lineName = lineName;
        this.stationPlatform = stationPlatform;
        this.message = message;
        this.dueTrams = dueTrams;
        this.lastUpdate = lastUpdate;
        this.displayId = displayId;
        this.location = location;
    }

    public StationDepartureInfoDTO(TramStationDepartureInfo info) {
        this(info.getLine().name(),
                IdForDTO.createFor(info.getStationPlatform()),
                info.getMessage(),
                mapDueTrams(info.getDueTrams(), info.getLastUpdate()),
                info.getLastUpdate(),
                info.getDisplayId(),
                info.getStation().getName());
    }

    public StationDepartureInfoDTO() {
        // deserialisation
    }

    private static List<DepartureDTO> mapDueTrams(List<UpcomingDeparture> dueTrams, LocalDateTime lastUpdated) {
        return dueTrams.stream().map(dueTram ->
                new DepartureDTO(dueTram.getDisplayLocation(), dueTram, lastUpdated)).collect(Collectors.toList());
    }

    public String getLineName() {
        return lineName;
    }

    public IdForDTO getStationPlatform() {
        return stationPlatform;
    }

    public String getMessage() {
        return message;
    }

    public List<DepartureDTO> getDueTrams() {
        return dueTrams;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateTimeFormatForJson)
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String getDisplayId() {
        return displayId;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationDepartureInfoDTO that = (StationDepartureInfoDTO) o;
        return lineName.equals(that.lineName) &&
                stationPlatform.equals(that.stationPlatform) &&
                message.equals(that.message) &&
                dueTrams.equals(that.dueTrams) &&
                lastUpdate.equals(that.lastUpdate) &&
                displayId.equals(that.displayId) &&
                location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineName, stationPlatform, message, dueTrams, lastUpdate, displayId, location);
    }

    @Override
    public String toString() {
        return "StationDepartureInfoDTO{" +
                "lineName='" + lineName + '\'' +
                ", stationPlatform='" + stationPlatform + '\'' +
                ", message='" + message + '\'' +
                ", dueTrams=" + dueTrams +
                ", lastUpdate=" + lastUpdate +
                ", displayId='" + displayId + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
