package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class SimpleStageDTO {
    private LocationRefWithPosition firstStation;
    private LocationRefWithPosition lastStation;
    private LocationRefWithPosition actionStation;

    private LocalDate queryDate;
    private LocalDateTime firstDepartureTime;
    private LocalDateTime expectedArrivalTime;
    private long duration;

    private String headSign;

    private TransportMode mode;
    private int passedStops;
    private String action;

    private RouteRefDTO route;

    public SimpleStageDTO(LocationRefWithPosition firstStation, LocationRefWithPosition lastStation, LocationRefWithPosition actionStation,
                          LocalDateTime firstDepartureTime, LocalDateTime expectedArrivalTime, Duration duration,
                          String headSign, TransportMode mode, int passedStops,
                          RouteRefDTO route, TravelAction action, TramDate queryDate) {
        this.firstStation = firstStation;
        this.lastStation = lastStation;
        this.actionStation = actionStation;
        this.firstDepartureTime = firstDepartureTime;
        this.expectedArrivalTime = expectedArrivalTime;
        this.headSign = headSign;
        this.mode = mode;
        this.passedStops = passedStops;
        this.route = route;
        this.action = action.toString();
        this.queryDate = queryDate.toLocalDate();

        // todo seconds?
        this.duration = duration.toMinutes();

    }

    public SimpleStageDTO() {
        // deserialisation
    }

    public String getHeadSign() {
        return headSign;
    }

    public LocationRefWithPosition getActionStation() {
        return actionStation;
    }

    public LocationRefWithPosition getLastStation() {
        return lastStation;
    }

    public LocationRefWithPosition getFirstStation() {
        return firstStation;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateTimeFormatForJson)
    public LocalDateTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateTimeFormatForJson)
    public LocalDateTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateFormatForJson)
    public LocalDate getQueryDate() {
        return queryDate;
    }

    public long getDuration() {
        return duration;
    }

    public TransportMode getMode() {
        return mode;
    }

    public int getPassedStops() {
        return passedStops;
    }

    public RouteRefDTO getRoute() {
        return route;
    }

    public String getAction() {
        return action;
    }

    @JsonIgnore
    public boolean getHasPlatform() {
        return false;
    }

    @Override
    public String toString() {
        return "SimpleStageDTO{" +
                "firstStation=" + firstStation.getId() +
                ", lastStation=" + lastStation.getId() +
                ", actionStation=" + actionStation.getId() +
                ", queryDate=" + queryDate +
                ", firstDepartureTime=" + firstDepartureTime +
                ", expectedArrivalTime=" + expectedArrivalTime +
                ", duration=" + duration +
                ", headSign='" + headSign + '\'' +
                ", mode=" + mode +
                ", passedStops=" + passedStops +
                ", action='" + action + '\'' +
                ", route=" + route +
                '}';
    }

}
