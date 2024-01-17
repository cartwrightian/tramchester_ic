package com.tramchester.domain;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.mappers.Geography;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class StationToStationConnection {
    private final StationPair pair;
    private final EnumSet<TransportMode> linkingModes;
    private final Quantity<Length> distanceBetweenInMeters;
    private final Duration connectionTime;

    public StationToStationConnection(Station begin, Station end, EnumSet<TransportMode> linkingModes, Quantity<Length> distanceBetweenInMeters,
                                      Duration connectionTime) {
        this.distanceBetweenInMeters = distanceBetweenInMeters;
        this.connectionTime = connectionTime;
        this.pair = StationPair.of(begin, end);
        this.linkingModes = linkingModes;
    }

    public static StationToStationConnection createForWalk(Station begin, Station end, EnumSet<TransportMode> linkingModes, Geography geography) {
        final Quantity<Length> distance = geography.getDistanceBetweenInMeters(begin, end);
        final Duration walkingDuration = geography.getWalkingDuration(begin, end);
        return new StationToStationConnection(begin, end, linkingModes, distance, walkingDuration);
    }

    public Station getBegin() {
        return pair.getBegin();
    }

    public Station getEnd() {
        return pair.getEnd();
    }

    @Override
    public String toString() {
        return "StationToStationConnection{" +
                "pair=" + pair +
                ", linkingModes=" + linkingModes +
                ", distanceBetweenInMeters=" + distanceBetweenInMeters +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationToStationConnection that = (StationToStationConnection) o;

        if (!pair.equals(that.pair)) return false;
        return linkingModes.equals(that.linkingModes);
    }

    @Override
    public int hashCode() {
        int result = pair.hashCode();
        result = 31 * result + linkingModes.hashCode();
        return result;
    }

    /***
     * The transport modes that link these two stations
     * NOT the modes of the stations themselves which might be subset of linking modes
     * @return The transport modes that link these two stations i.e. Walk
     */
    public Set<TransportMode> getLinkingModes() {
        return linkingModes;
    }

    public boolean hasValidLatlongs() {
        return pair.getBegin().getLatLong().isValid() && pair.getEnd().getLatLong().isValid();
    }

    public Quantity<Length> getDistanceInMeters() {
        return distanceBetweenInMeters;
    }

    public Duration getConnectionTime() {
        return connectionTime;
    }

    /***
     * The transport modes of the contained stations, not the modes linking the stations
     * @return set of modes
     */
    public EnumSet<TransportMode> getContainedModes() {
        Set<TransportMode> modes = new HashSet<>(pair.getBegin().getTransportModes());
        modes.addAll(pair.getEnd().getTransportModes());
        return EnumSet.copyOf(modes);
    }
}
