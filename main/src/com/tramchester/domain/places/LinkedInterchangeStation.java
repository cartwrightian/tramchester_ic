package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/***
 * These interchanges stations has an implicit direction
 * Dropoffs at the original =>> Pickups at either origin or linked
 */
public class LinkedInterchangeStation implements InterchangeStation {
    private final Set<StationToStationConnection> links;
    private final Station origin;
    private final EnumSet<TransportMode> allModes;

    public LinkedInterchangeStation(final StationToStationConnection stationLink) {
        links = new HashSet<>();
        links.add(stationLink);
        origin = stationLink.getBegin();
        final Set<TransportMode> collectedModes = links.stream().
                flatMap(links -> links.getContainedModes().stream()).
                collect(Collectors.toSet());
        this.allModes = EnumSet.copyOf(collectedModes);
    }

    @Override
    public boolean isMultiMode() {
        return allModes.size() > 1;
    }

    /***
     * See above note on the 'direction' for a linked interchange
     * @return only the dropoffs for the origin
     */
    @Override
    public Set<Route> getDropoffRoutes() {
        return origin.getDropoffRoutes();
    }


    /***
     * See above note on the 'direction' for a linked interchange
     * @return pickups for both the origin and all linked stations
     */
    @Override
    public Set<Route> getPickupRoutes() {
        final Set<Route> pickUps = new HashSet<>(origin.getPickupRoutes());
        final Set<Route> otherEnd = links.stream().map(StationToStationConnection::getEnd).
                flatMap(station -> station.getPickupRoutes().stream()).collect(Collectors.toSet());
        pickUps.addAll(otherEnd);
        return pickUps;
    }

    @Override
    public boolean servesRoutePickup(final Route route) {
        if (origin.servesRoutePickup(route)) {
            return true;
        }
        return links.stream().anyMatch(link -> link.getEnd().servesRoutePickup(route));
    }

    @Override
    public boolean servesRouteDropOff(final Route route) {
        return origin.servesRouteDropOff(route);
    }

    @Override
    public InterchangeType getType() {
        return InterchangeType.NeighbourLinks;
    }

    @Override
    public Station getStation() {
        return origin;
    }

    @Override
    public LocationId<?> getLocationId() {
        return origin.getLocationId();
    }

    @Override
    public Set<Station> getAllStations() {
        final Set<Station> result = new HashSet<>();
        result.add(origin);
        links.forEach(link -> result.add(link.getEnd()));
        return result;
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return allModes;
    }

    public void addLink(final StationToStationConnection stationLink) {
        if (!stationLink.getBegin().equals(origin)) {
            throw new RuntimeException(format("Attempt to add a stationlink (%s) that does not match origin %s", stationLink, origin));
        }
        if (links.contains(stationLink)) {
            throw new RuntimeException(format("Attempt to add duplicated link %s to %s", stationLink, links));
        }
        links.add(stationLink);
    }

    @Override
    public String toString() {
        return "LinkedInterchangeStation{" +
                "links=" + links +
                ", origin=" + origin.getId() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedInterchangeStation that = (LinkedInterchangeStation) o;
        return links.equals(that.links) && origin.equals(that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(links, origin);
    }

    public Set<Station> getLinked() {
        return links.stream().map(StationToStationConnection::getEnd).collect(Collectors.toSet());
    }

    @Override
    public IdFor<Station> getId() {
        return origin.getId();
    }
}
