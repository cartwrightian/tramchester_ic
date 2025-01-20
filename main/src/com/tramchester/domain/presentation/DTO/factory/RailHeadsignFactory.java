package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.dataimport.rail.repository.RailRouteIds;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.naptan.NaptanRepository;
import jakarta.inject.Inject;

@LazySingleton
public class RailHeadsignFactory {
    final RailRouteIdRepository railRouteIdRepository;
    private final NaptanRepository naptanRepository;

    @Inject
    public RailHeadsignFactory(RailRouteIdRepository railRouteIdRepository, NaptanRepository naptanRepository) {
        this.railRouteIdRepository = railRouteIdRepository;
        this.naptanRepository = naptanRepository;
    }

    public String getFor(final TransportStage<?, ?> source) {
        final Route route = source.getRoute();

        if (route.getTransportMode() != TransportMode.Train) {
            throw new RuntimeException("Train stages only!");
        }

        final Agency agency = route.getAgency();

        final RailRouteIds.RailRouteCallingPointsWithRouteId railCallingPoints = railRouteIdRepository.find(agency.getId(), route.getId());

        final IdFor<Station> endId = railCallingPoints.getBeginEnd().getEndId();

        final String name = getName(endId);

        return String.format("%s train towards %s", agency.getName(), name);
    }

    private String getName(final IdFor<Station> stationId) {
        if (naptanRepository.containsTiploc(stationId)) {
            final NaptanRecord record = naptanRepository.getForTiploc(stationId);
            return record.getCommonName();
        }
        return IdForDTO.createFor(stationId).getActualId();
    }
}
