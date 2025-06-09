package com.tramchester.domain.places;

import com.tramchester.domain.HasRoutes;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
        origin = stationLink.getBegin();

        links = new HashSet<>();
        links.add(stationLink);
        final Set<TransportMode> collectedModes = links.stream().
                flatMap(links -> links.getContainedModes().stream()).
                collect(Collectors.toSet());
        this.allModes = EnumSet.copyOf(collectedModes);
    }

    public void addLink(final StationToStationConnection stationLink) {
        if (!stationLink.getBegin().equals(origin)) {
            throw new RuntimeException(format("Attempt to add a stationlink (%s) that does not match origin %s", stationLink, origin));
        }
        if (links.contains(stationLink)) {
            throw new RuntimeException(format("Attempt to add duplicated link %s to %s", stationLink, links));
        }
        links.add(stationLink);
        allModes.addAll(stationLink.getContainedModes());
    }

    @Override
    public boolean isMultiMode() {
        return allModes.size() > 1;
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return merge(HasRoutes::getDropoffRoutes);
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return merge(HasRoutes::getPickupRoutes);
    }

    @Override
    public boolean servesRoutePickup(final Route route) {
        return fold(station -> station.servesRoutePickup(route));
    }

    @Override
    public boolean servesRouteDropOff(final Route route) {
        return fold(station -> station.servesRouteDropOff(route));
    }

    private boolean fold(final Function<Station, Boolean> check) {
        if (check.apply(origin)) {
            return true;
        }
        return links.stream().anyMatch(link -> check.apply(link.getEnd()));
    }

    private Set<Route> merge(final Function<Station, Set<Route>> getRoutes) {
        final Set<Route> result = new HashSet<>(getRoutes.apply(origin));
        links.forEach(link -> result.addAll(getRoutes.apply(link.getEnd())));
        return result;
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

    @Override
    public boolean anyOverlapWith(final EnumSet<TransportMode> other) {
        return TransportMode.anyIntersection(allModes, other);
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
        if (!(o instanceof LinkedInterchangeStation that)) return false;
        return Objects.equals(origin, that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(origin);
    }

    public Set<Station> getLinked() {
        return links.stream().map(StationToStationConnection::getEnd).collect(Collectors.toSet());
    }

    @Override
    public IdFor<Station> getId() {
        return origin.getId();
    }
}
