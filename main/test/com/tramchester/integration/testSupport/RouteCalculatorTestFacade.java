package com.tramchester.integration.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.KnownLocality;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class RouteCalculatorTestFacade {
    private final RouteCalculator routeCalculator;
    private final StationRepository stationRepository;
    private final MutableGraphTransaction txn;
    private final StationGroupsRepository stationGroupsRepository;

    public  RouteCalculatorTestFacade(ComponentContainer componentContainer, MutableGraphTransaction txn) {
        this.routeCalculator = componentContainer.get(RouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        this.txn = txn;
    }

    public List<Journey> calculateRouteAsList(FakeStation start, FakeStation end, JourneyRequest journeyRequest) {
        return calculateRouteAsList(start.from(stationRepository), end.from(stationRepository), journeyRequest);
    }

    public List<Journey> calculateRouteAsList(IdFor<Station> startId, IdFor<Station> destId, JourneyRequest request) {
        return calculateRouteAsList(getFor(startId), getFor(destId), request);
    }

    public List<Journey> calculateRouteAsList(FakeStation start, StationGroup end, JourneyRequest journeyRequest) {
        return calculateRouteAsList(start.from(stationRepository), end, journeyRequest);
    }

    public List<Journey> calculateRouteAsList(KnownLocality begin, KnownLocality end, JourneyRequest request) {
        return calculateRouteAsList(begin.from(stationGroupsRepository), end.from(stationGroupsRepository), request);
    }

    public @NotNull List<Journey> calculateRouteAsList(Location<?> start, Location<?> dest, JourneyRequest request) {
        Stream<Journey> stream = routeCalculator.calculateRoute(txn, start, dest, request);
        List<Journey> result = stream.toList();
        stream.close();
        return result;
    }

    private Station getFor(IdFor<Station> id) {
        return stationRepository.getStationById(id);
    }

}
