package com.tramchester.domain.presentation.DTO.query;

import com.fasterxml.jackson.annotation.*;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@JsonTypeName("JourneyQuery")
public class JourneyQueryDTO  {
    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("startType")
    private LocationType startType;

    @JsonProperty("startId")
    private IdForDTO startId;

    @JsonProperty("destType")
    private LocationType destType;

    @JsonProperty("destId")
    private IdForDTO destId;

    @JsonProperty("arriveBy")
    private boolean arriveBy;

    @JsonProperty("maxChanges")
    private int maxChanges;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("maxNumResults")
    private Integer maxNumResults;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("modes")
    private Set<TransportMode> modes;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("diagnostics")
    private Boolean diagnostics;

    public JourneyQueryDTO() {
        modes = Collections.emptySet();
        // deserialisation
    }

    public JourneyQueryDTO(LocalDate date, LocalTime time, LocationType startType, IdForDTO startId, LocationType destType,
                           IdForDTO destId, boolean arriveBy, int maxChanges, Boolean diagnostics) {

        this.date = date;
        this.time = time;
        this.startType = startType;
        this.startId = startId;
        this.destType = destType;
        this.destId = destId;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
        this.diagnostics = diagnostics;
        this.modes = Collections.emptySet();
        this.maxNumResults = null;

        if (maxChanges>2) {
            throw new RuntimeException("Finding out where this is too high");
        }
    }

    public static JourneyQueryDTO create(LocalDate date, TramTime time, Location<?> start, Location<?> dest,
                                         boolean arriveBy, int maxChanges, boolean diagnostics) {

        IdForDTO startId = IdForDTO.createFor(start);
        IdForDTO destId = IdForDTO.createFor(dest);
        LocationType startType = start.getLocationType();
        LocationType destType = dest.getLocationType();

        return new JourneyQueryDTO(date, time.asLocalTime(), startType, startId, destType, destId, arriveBy, maxChanges, diagnostics);

    }

    public static JourneyQueryDTO create(TramDate date, TramTime time, Location<?> start, Location<?> dest,
                                         boolean arriveBy, int maxChanges) {
        return create(date.toLocalDate(), time, start, dest, arriveBy, maxChanges, false);
    }

    @JsonIgnore
    public static JourneyRequest toJourneyRequest(final TramchesterConfig config, final JourneyQueryDTO dto) {
        // if no modes provided then default to all modes currently configured
        final EnumSet<TransportMode> modes = dto.getModes().isEmpty() ? config.getTransportModes() : EnumSet.copyOf(dto.getModes());
        final TramDate date = dto.getTramDate();
        final LocalTime time = dto.getTime();

        TramTime queryTime = TramTime.ofHourMins(time);
        if (queryTime.between(TramTime.of(0,0), TramTime.of(4,0))) {
            queryTime = TramTime.nextDay(queryTime);
        }

        final boolean arriveBy = dto.isArriveBy();
        final int maxChanges = dto.getMaxChanges();
        final TramDuration maxJourneyDuration = TramDuration.ofMinutes(config.getMaxJourneyDuration());

        int maxNumberResults = (dto.getMaxNumResults()==null) ? config.getMaxNumberResults() : dto.getMaxNumResults();

        JourneyRequest journeyRequest = new JourneyRequest(date, queryTime, arriveBy, JourneyRequest.MaxNumberOfChanges.of(maxChanges),
                maxJourneyDuration, maxNumberResults, modes);

        if (dto.diagnostics!=null) {
            journeyRequest.setDiag(dto.diagnostics);
        }
        return journeyRequest;
    }

    @Override
    public String toString() {
        return "JourneyQueryDTO{" +
                "date=" + date +
                ", time=" + time +
                ", startType=" + startType +
                ", startId=" + startId +
                ", destType=" + destType +
                ", destId=" + destId +
                ", arriveBy=" + arriveBy +
                ", maxChanges=" + maxChanges +
                ", maxNumResults= " + maxNumResults +
                ", modes=" + modes +
                ", diagnostics=" + diagnostics +
                '}';
    }

    @JsonIgnore
    public TramDate getTramDate() {
        return TramDate.of(date);
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public LocationType getStartType() {
        return startType;
    }

    public IdForDTO getStartId() {
        return startId;
    }

    public LocationType getDestType() {
        return destType;
    }

    public IdForDTO getDestId() {
        return destId;
    }

    public boolean isArriveBy() {
        return arriveBy;
    }

    public int getMaxChanges() {
        return maxChanges;
    }

    public Integer getMaxNumResults() {
        return maxNumResults;
    }

    @JsonIgnore
    public boolean valid() {
        return startId!=null && startType!=null && destId!=null && destType!=null && date!=null
                && modes!=null;
    }

    public void setModes(Set<TransportMode> modes) {
        this.modes = modes;
    }

    public Set<TransportMode> getModes() {
        return modes;
    }

    public Boolean getDiagnostics() {
        if (diagnostics==null) {
            return false;
        }
        return diagnostics;
    }

    public void setMaxNumResults(int value) {
        maxNumResults = value;
    }
}
