package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.domain.transportStages.WalkingStage;

import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform?

@LazySingleton
public class StageDTOFactory {

    private final DTOFactory stationDTOFactory;

    @Inject
    public StageDTOFactory(DTOFactory stationDTOFactory) {

        this.stationDTOFactory = stationDTOFactory;
    }

    public SimpleStageDTO build(final TransportStage<?,?> source, final TravelAction travelAction, final TramDate queryDate) {

        final LocationRefWithPosition firstStation = stationDTOFactory.createLocationRefWithPosition(source.getFirstStation());
        final LocationRefWithPosition lastStation = stationDTOFactory.createLocationRefWithPosition(source.getLastStation());
        final LocationRefWithPosition actionStation = stationDTOFactory.createLocationRefWithPosition(source.getActionStation());
        final LocalDateTime firstDepartureTime = source.getFirstDepartureTime().toDate(queryDate);
        final LocalDateTime expectedArrivalTime = source.getExpectedArrivalTime().toDate(queryDate);

        final Route route = source.getRoute();
        final RouteRefDTO routeRefDTO = new RouteRefDTO(route);

        final Duration duration = source.getDuration();
        if (source instanceof WalkingStage<?,?> || source instanceof ConnectingStage<?,?>) {
            return new SimpleStageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime, duration,
                    source.getHeadSign(), source.getMode(), source.getPassedStopsCount(),
                    routeRefDTO, travelAction, queryDate);
        }

        final IdForDTO tripId = new IdForDTO(source.getTripId());
        if (source.hasBoardingPlatform()) {
            final PlatformDTO boardingPlatform = new PlatformDTO(source.getBoardingPlatform());

            return new VehicleStageDTO(firstStation,
                    lastStation,
                    actionStation,
                    boardingPlatform,
                    firstDepartureTime, expectedArrivalTime,
                    duration, source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        } else {
            return new VehicleStageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime,
                    duration, source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        }
    }

}
