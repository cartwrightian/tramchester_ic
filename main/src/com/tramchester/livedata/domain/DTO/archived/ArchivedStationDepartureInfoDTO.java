package com.tramchester.livedata.domain.DTO.archived;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tramchester.config.TramchesterConfig;

import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("unused")
public class ArchivedStationDepartureInfoDTO {
    private String lineName;
    private String stationPlatform;
    private String message;
    private List<ArchivedDepartureDTO> dueTrams;
    private LocalDateTime lastUpdate;
    private String displayId;
    private String location;

    public ArchivedStationDepartureInfoDTO() {
        // deserialisation
    }

    public ArchivedStationDepartureInfoDTO(String lineName, String stationPlatform, String message,
                                           List<ArchivedDepartureDTO> dueTrams, LocalDateTime lastUpdate,
                                           String displayId, String location) {
        this.lineName = lineName;
        this.stationPlatform = stationPlatform;
        this.message = message;
        this.dueTrams = dueTrams;
        this.lastUpdate = lastUpdate;
        this.displayId = displayId;
        this.location = location;
    }

    public String getLineName() {
        return lineName;
    }

    public String getStationPlatform() {
        return stationPlatform;
    }

    public String getMessage() {
        return message;
    }

    public List<ArchivedDepartureDTO> getDueTrams() {
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
