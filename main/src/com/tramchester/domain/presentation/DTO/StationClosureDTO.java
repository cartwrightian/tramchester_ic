package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.closures.Closure;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.TramTimeJsonSerializer;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StationClosureDTO {
    private List<LocationRefDTO> stations;
    private LocalDate begin;
    private LocalDate end;
    private Boolean fullyClosed;
    private Boolean allDay;
    private TramTime beginTime;
    private TramTime endTime;

    public static StationClosureDTO from(StationClosures closure, List<LocationRefDTO> refs) {
        TimeRange timeRange = closure.hasTimeRange() ? closure.getTimeRange() : TimeRange.AllDay();
        DateRange dateRange = closure.getDateRange();
        return new StationClosureDTO(dateRange.getStartDate(), dateRange.getEndDate(), timeRange.getStart(),
                timeRange.getEnd(), timeRange.allDay(), refs, closure.isFullyClosed());
    }

    public static StationClosureDTO from(Closure closure) {
        DateRange dateRange = closure.getDateRange();
        TimeRange timeRange = closure.getTimeRange();
        Set<Station> stations = closure.getStations();
        List<LocationRefDTO> refs = stations.stream().map(LocationRefDTO::new).collect(Collectors.toList());

        return new StationClosureDTO(dateRange.getStartDate(), dateRange.getEndDate(), timeRange.getStart(),
                timeRange.getEnd(), timeRange.allDay(), refs, closure.isFullyClosed());
    }

    private StationClosureDTO(TramDate begin, TramDate end, TramTime beginTime, TramTime endTime, boolean allDay, List<LocationRefDTO> stations, boolean fullyClosed) {
        this.stations = stations;
        this.begin = begin.toLocalDate();
        this.end = end.toLocalDate();
        this.fullyClosed = fullyClosed;
        this.allDay = allDay;
        if (!this.allDay) {
            this.beginTime = beginTime;
            this.endTime = endTime;
        }
    }

    @SuppressWarnings("unused")
    public StationClosureDTO() {
        // deserialisation
    }



    public List<LocationRefDTO> getStations() {
        return stations;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateFormatForJson)
    public LocalDate getBegin() {
        return begin;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateFormatForJson)
    public LocalDate getEnd() {
        return end;
    }

    public Boolean getFullyClosed() {
        return fullyClosed;
    }

    public Boolean getAllDay() {
        return allDay;
    }

    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public TramTime getBeginTime() {
        return beginTime;
    }

    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public TramTime getEndTime() {
        return endTime;
    }
}
