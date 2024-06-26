package com.tramchester.livedata.tfgm;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TramStationDepartureInfo {

    private static final String NO_MESSAGE = "<no message>";

    private final Lines line;
    private Platform stationPlatform;
    private final String message;
    private final List<UpcomingDeparture> dueTrams;
    private final LocalDateTime lastUpdate;
    private final String displayId;
    private final Station station;
    private final LineDirection direction;

    // station code here is the actocode
    public TramStationDepartureInfo(String displayId, Lines line, LineDirection direction,
                                    Station station, String message, LocalDateTime lastUpdate) {
        this.displayId = displayId;
        this.line = line;
        this.direction = direction;
        this.station = station;
        if (invalidMessage(message)) {
            this.message= "";
        } else {
            this.message = message;
        }
        this.stationPlatform = null;
        this.lastUpdate = lastUpdate;
        dueTrams = new LinkedList<>();
    }

    private boolean invalidMessage(String message) {
        return NO_MESSAGE.equals(message) || scrollingDisplay(message);
    }

    private boolean scrollingDisplay(String message) {
        return message.startsWith("^F0Next");
    }

    public Lines getLine() {
        return line;
    }

    public LineDirection getDirection() {
        return direction;
    }

    public boolean hasStationPlatform() {
        return stationPlatform!=null;
    }

    public void setStationPlatform(Platform stationPlatform) {
        this.stationPlatform = stationPlatform;
    }

    public Platform getStationPlatform() {
        return stationPlatform;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasDueTrams() {
        return !dueTrams.isEmpty();
    }

    public List<UpcomingDeparture> getDueTrams() {
        return dueTrams;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void addDueTram(UpcomingDeparture dueTram) {
        dueTrams.add(dueTram);
    }

    public String getDisplayId() {
        return displayId;
    }

    public Station getStation() {
        return station;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TramStationDepartureInfo that = (TramStationDepartureInfo) o;
        return line.equals(that.line) &&
                stationPlatform.equals(that.stationPlatform) &&
                message.equals(that.message) &&
                dueTrams.equals(that.dueTrams) &&
                lastUpdate.equals(that.lastUpdate) &&
                displayId.equals(that.displayId) &&
                station.equals(that.station) &&
                direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, stationPlatform, message, dueTrams, lastUpdate, displayId, station, direction);
    }

    @Override
    public String toString() {
        return "StationDepartureInfo{" +
                "line='" + line + '\'' +
                ", stationPlatform='" + stationPlatform + '\'' +
                ", location='" + HasId.asId(station) + '\'' +
                ", message='" + message + '\'' +
                ", dueTrams=" + dueTrams +
                ", lastUpdate=" + lastUpdate +
                ", displayId='" + displayId + '\'' +
                ", direction=" + direction +
                '}';
    }

}
