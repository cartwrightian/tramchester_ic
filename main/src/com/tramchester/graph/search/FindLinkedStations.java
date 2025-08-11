package com.tramchester.graph.search;

import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;

import java.util.EnumSet;
import java.util.Set;

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

    protected StationToStationConnection createConnection(final GraphRelationship relationship) {

        final IdFor<Station> startId = relationship.getStartStationId();
        final IdFor<Station> endId = relationship.getEndStationId();

        final Station start = stationRepository.getStationById(startId);
        final Station end = stationRepository.getStationById(endId);

        final EnumSet<TransportMode> modes = relationship.getTransportModes();

        return StationToStationConnection.createForWalk(start, end, modes, StationToStationConnection.LinkType.Linked, geography);
    }
}
