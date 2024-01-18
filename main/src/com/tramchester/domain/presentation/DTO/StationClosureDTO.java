package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;

import java.time.LocalDate;
import java.util.List;

public class StationClosureDTO {
    private List<LocationRefDTO> stations;
    private LocalDate begin;
    private LocalDate end;
    private Boolean fullyClosed;

    public StationClosureDTO(DateRange dateRange, List<LocationRefDTO> refs, boolean fullyClosed) {
        this(dateRange.getStartDate(), dateRange.getEndDate(), refs, fullyClosed);
    }

    private StationClosureDTO(TramDate begin, TramDate end, List<LocationRefDTO> stations, boolean fullyClosed) {
        this.stations = stations;
        this.begin = begin.toLocalDate();
        this.end = end.toLocalDate();
        this.fullyClosed = fullyClosed;
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
}
