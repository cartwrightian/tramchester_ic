package com.tramchester.domain.places;

import com.tramchester.domain.HasRoutes;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.Interchanges;
import org.apache.commons.lang3.tuple.Pair;

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
    private final ImmutableEnumSet<TransportMode> sameModeChangeOverride;

    private LinkedInterchangeStation(final StationToStationConnection stationLink, final ImmutableEnumSet<TransportMode> sameModeChangeOverride) {
        this.origin = stationLink.getBegin();

        this.links = new HashSet<>();
        this.allModes = EnumSet.noneOf(TransportMode.class);
        this.sameModeChangeOverride = sameModeChangeOverride;

        addLink(stationLink);
    }

    public LinkedInterchangeStation(final StationToStationConnection stationLink) {
        this(stationLink, ImmutableEnumSet.noneOf(TransportMode.class));
    }

    public LinkedInterchangeStation(InterchangeStation existing, StationToStationConnection stationLink) {
        this(stationLink, existing.getTransportModes());
    }

    public void addLink(final StationToStationConnection stationLink) {
        if (!stationLink.getBegin().equals(origin)) {
            throw new RuntimeException(format("Attempt to add a stationlink (%s) that does not match origin %s", stationLink, origin));
        }
        if (links.contains(stationLink)) {
            throw new RuntimeException(format("Attempt to add duplicated link %s to %s", stationLink, links));
        }
        links.add(stationLink);
        stationLink.getContainedModes().addAllTo(allModes);
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
        return (allModes.size() > 1) ? InterchangeType.NeighbourLinksMultimodal : InterchangeType.NeighbourLinksSingleMode;
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
    public boolean isAvailableSameMode(final ImmutableEnumSet<TransportMode> modes) {
        if (sameModeChangeOverride.anyIntersectionWith(modes)) {
            return true;
        }

        // enough links of the same mode over threshold
        // TODO could be pre-calculated if causes performance issue
       return modes.stream().
               map(mode -> Pair.of(mode, linksWithMode(mode))).
               anyMatch(pair -> pair.getValue() >= Interchanges.getLinkThreshhold(pair.getKey()));
    }

    private long linksWithMode(TransportMode mode) {
        return links.stream().filter(link -> link.getContainedModes().contains(mode)).count();
    }

    @Override
    public Set<Station> getAllStations() {
        final Set<Station> result = new HashSet<>();
        result.add(origin);
        links.forEach(link -> result.add(link.getEnd()));
        return result;
    }

    @Override
    public ImmutableEnumSet<TransportMode> getTransportModes() {
        return ImmutableEnumSet.copyOf(allModes);
    }

    @Override
    public boolean anyOverlapWith(final ImmutableEnumSet<TransportMode> other) {
        return other.anyIntersectionWith(allModes);
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
