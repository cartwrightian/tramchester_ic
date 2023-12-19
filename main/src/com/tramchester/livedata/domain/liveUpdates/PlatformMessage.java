package com.tramchester.livedata.domain.liveUpdates;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

public class PlatformMessage {
    private final Platform stationPlatform;
    private final String text;
    private final LocalDateTime lastUpdate;
    private final Station station;
    private final String displayId;

    public PlatformMessage(Platform stationPlatform, String text, LocalDateTime lastUpdate, Station station, String displayId) {
        this.stationPlatform = stationPlatform;
        this.text = text;
        this.lastUpdate = lastUpdate;
        this.station = station;
        this.displayId = displayId;
    }

    public PlatformMessage(TramStationDepartureInfo departureInfo) {
        this(departureInfo.getStationPlatform(), departureInfo.getMessage(), departureInfo.getLastUpdate(),
                departureInfo.getStation(), departureInfo.getDisplayId());
    }

    @NotNull
    public String getMessage() {
        return text;
    }

    public Station getStation() {
        return station;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String toString() {
        return "PlatformMessage{" +
                "stationPlatform=" + stationPlatform +
                ", message='" + text + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", station=" + HasId.asId(station) +
                ", displayId='" + displayId + '\'' +
                '}';
    }

    public String getDisplayId() {
        return displayId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlatformMessage that = (PlatformMessage) o;
        return Objects.equals(stationPlatform, that.stationPlatform) && Objects.equals(text, that.text)
                && Objects.equals(lastUpdate, that.lastUpdate) && Objects.equals(station, that.station) && Objects.equals(displayId, that.displayId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationPlatform, text, lastUpdate, station, displayId);
    }
}
