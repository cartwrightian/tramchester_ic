package com.tramchester.domain.presentation.DTO;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonTypeName("journey")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use= JsonTypeInfo.Id.NAME)
public class JourneyDTO {

    private LocationRefWithPosition begin;
    private List<SimpleStageDTO> stages;
    private LocalDateTime expectedArrivalTime; // needed to handle 'next day' results
    private LocalDateTime firstDepartureTime;  // needed to handle 'next day' results
    private List<LocationRefWithPosition> changeStations;
    private TramTime queryTime;
    private List<LocationRefWithPosition> path;
    private LocalDate queryDate;
    private int index;

    public JourneyDTO() {
        // Deserialization
    }

    public JourneyDTO(LocationRefWithPosition begin, List<SimpleStageDTO> stages,
                      LocalDateTime expectedArrivalTime, LocalDateTime firstDepartureTime,
                      List<LocationRefWithPosition> changeStations, TramTime queryTime,
                      List<LocationRefWithPosition> path, TramDate queryDate, int index) {
        this(begin, stages, expectedArrivalTime, firstDepartureTime, changeStations, queryTime,
                path, queryDate.toLocalDate(), index);
    }

    public JourneyDTO(LocationRefWithPosition begin, List<SimpleStageDTO> stages,
                      LocalDateTime expectedArrivalTime, LocalDateTime firstDepartureTime,
                      List<LocationRefWithPosition> changeStations, TramTime queryTime,
                      List<LocationRefWithPosition> path, LocalDate queryDate, int index) {
        this.begin = begin;
        this.stages = stages;
        this.expectedArrivalTime = expectedArrivalTime;
        this.firstDepartureTime = firstDepartureTime;
        this.changeStations = changeStations;
        this.queryTime = queryTime;
        this.path = path;
        this.queryDate = queryDate;
        this.index = index;
    }

    public List<SimpleStageDTO> getStages() {
        return stages;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public LocationRefDTO getBegin() {
        return begin;
    }

    public List<LocationRefWithPosition> getChangeStations() {
        return changeStations;
    }

    @Override
    public String toString() {
        return "JourneyDTO{" +
                "begin=" + begin.getId() +
                ", stages=" + stages +
                ", expectedArrivalTime=" + expectedArrivalTime +
                ", firstDepartureTime=" + firstDepartureTime +
                ", changeStations=" + changeStations +
                ", queryTime=" + queryTime +
                '}';
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getQueryTime() {
        return queryTime;
    }

    public List<LocationRefWithPosition> getPath() {
        return path;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateFormatForJson)
    public LocalDate getQueryDate() {
        return queryDate;
    }

    public int getIndex() {
        return index;
    }
}
