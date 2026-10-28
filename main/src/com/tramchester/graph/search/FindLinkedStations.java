package com.tramchester.graph.search;

import com.tramchester.domain.Service;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// TODO Now only one impl get have implementation here.....
public abstract class  FindLinkedStations {
    private final StationRepository stationRepository;
    private final Geography geography;

    protected FindLinkedStations(StationRepository stationRepository, Geography geography) {
        this.stationRepository = stationRepository;
        this.geography = geography;
    }

    // supports visualisation of the transport network
    public abstract Set<StationToStationConnection> findLinkedFor(TransportMode mode);

    public abstract IdSet<Station> atLeastNLinkedStations(TransportMode mode, int threshhold);

    protected StationToStationConnection createConnection(final GraphRelationship relationship, final GraphTransaction txn) {

        final IdFor<Station> startId = relationship.getStartStationId(txn);
        final IdFor<Station> endId = relationship.getEndStationId(txn);

        final Station start = stationRepository.getStationById(startId);
        final Station end = stationRepository.getStationById(endId);

        final ImmutableEnumSet<TransportMode> modes = relationship.getTransportModes();

        return StationToStationConnection.createForWalk(start, end, modes, StationToStationConnection.LinkType.Linked, geography);
    }

    public abstract List<IdSet<Service>> serviceIdsPerLink(Station station);

    public IdSet<Service> atLeastNLinkedOverlappedServices(int threshold, Station station) {
        List<IdSet<Service>> servicesPerLinks = serviceIdsPerLink(station);

        Map<IdFor<Service>, AtomicInteger> totals = new HashMap<>();

        for (int i = 0; i < servicesPerLinks.size(); i++) {
            IdSet<Service> servicesForLink = servicesPerLinks.get(i);
            servicesForLink.forEach(serviceId -> {
                if (!totals.containsKey(serviceId)) {
                    totals.put(serviceId, new AtomicInteger());
                }
                totals.get(serviceId).incrementAndGet();
            });
        }

        return totals.entrySet().stream().
                filter(entry -> entry.getValue().get()>=threshold).
                map(Map.Entry::getKey).
                collect(IdSet.idCollector());

    }
}
