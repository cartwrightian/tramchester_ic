package com.tramchester.domain;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.mappers.Geography;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StationToStationConnection {

    public enum LinkType {
        Diversion,
        Linked,
        Neighbour
    }

    private final StationPair pair;
    private final EnumSet<TransportMode> linkingModes;
    private final LinkType linkType;
    private final Quantity<Length> distanceBetweenInMeters;
    private final Duration connectionTime;

    public StationToStationConnection(Station begin, Station end, EnumSet<TransportMode> linkingModes, LinkType linkType,
                                      Quantity<Length> distanceBetweenInMeters, Duration connectionTime) {
        this.linkType = linkType;
        this.distanceBetweenInMeters = distanceBetweenInMeters;
        this.connectionTime = connectionTime;
        this.pair = StationPair.of(begin, end);
        this.linkingModes = linkingModes;
    }

    public static StationToStationConnection createForWalk(Station begin, Station end, EnumSet<TransportMode> linkingModes,
                                                           LinkType linkType, Geography geography) {
        final Quantity<Length> distance = geography.getDistanceBetweenInMeters(begin, end);
        final Duration walkingDuration = geography.getWalkingDuration(begin, end);
        return new StationToStationConnection(begin, end, linkingModes, linkType, distance, walkingDuration);
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
                ", linkType=" + linkType +
                ", distanceBetweenInMeters=" + distanceBetweenInMeters +
                ", connectionTime=" + connectionTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationToStationConnection that = (StationToStationConnection) o;
        return Objects.equals(pair, that.pair) && Objects.equals(linkingModes, that.linkingModes)
                && linkType == that.linkType && Objects.equals(distanceBetweenInMeters, that.distanceBetweenInMeters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pair, linkingModes, linkType, distanceBetweenInMeters);
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
        final Set<TransportMode> modes = new HashSet<>(pair.getBegin().getTransportModes());
        modes.addAll(pair.getEnd().getTransportModes());
        return EnumSet.copyOf(modes);
    }

    public LinkType getLinkType() {
        return linkType;
    }
}
