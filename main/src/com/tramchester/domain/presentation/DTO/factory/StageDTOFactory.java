package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.BusReplacementRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.domain.transportStages.WalkingStage;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.LocalDateTime;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform?

@LazySingleton
public class StageDTOFactory {

    private final DTOFactory stationDTOFactory;
    private final RailHeadsignFactory railHeadsignFactory;
    private final BusReplacementRepository busReplacementRepository;

    @Inject
    public StageDTOFactory(DTOFactory stationDTOFactory, RailHeadsignFactory railHeadsignFactory, BusReplacementRepository busReplacementRepository) {
        this.stationDTOFactory = stationDTOFactory;
        this.railHeadsignFactory = railHeadsignFactory;
        this.busReplacementRepository = busReplacementRepository;
    }

    public SimpleStageDTO build(final TransportStage<?,?> source, final TravelAction travelAction, final TramDate queryDate) {
        final Route route = source.getRoute();

        TransportMode mode = source.getMode();
        if (mode==TransportMode.Tram) {
            if (Agency.IsMetrolink(route.getAgency().getId()) && busReplacementRepository.isReplacement(route.getId())) {
                mode=TransportMode.Bus;
            }
        }

        final LocationRefWithPosition firstStation = stationDTOFactory.createLocationRefWithPosition(source.getFirstStation());
        final LocationRefWithPosition lastStation = stationDTOFactory.createLocationRefWithPosition(source.getLastStation());
        final LocationRefWithPosition actionStation = stationDTOFactory.createLocationRefWithPosition(source.getActionStation());
        final LocalDateTime firstDepartureTime = source.getFirstDepartureTime().toDate(queryDate);
        final LocalDateTime expectedArrivalTime = source.getExpectedArrivalTime().toDate(queryDate);

        final RouteRefDTO routeRefDTO = new RouteRefDTO(route);

        final Duration duration = source.getDuration();
        if (source instanceof WalkingStage<?,?> || source instanceof ConnectingStage<?,?>) {
            return new SimpleStageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime, duration,
                    source.getHeadSign(), mode, source.getPassedStopsCount(),
                    routeRefDTO, travelAction, queryDate);
        }

        final IdForDTO tripId = new IdForDTO(source.getTripId());
        final String headsign = getHeadsignFor(source);
        if (source.hasBoardingPlatform()) {
            final PlatformDTO boardingPlatform = new PlatformDTO(source.getBoardingPlatform());

            return new VehicleStageDTO(firstStation,
                    lastStation,
                    actionStation,
                    boardingPlatform,
                    firstDepartureTime, expectedArrivalTime,
                    duration, headsign,
                    mode,
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        } else {
            return new VehicleStageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime,
                    duration, headsign,
                    mode,
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        }
    }

    private String getHeadsignFor(TransportStage<?, ?> source) {
        if (source.getMode()== TransportMode.Train) {
            return railHeadsignFactory.getFor(source);
        } else {
            return source.getHeadSign();
        }
    }

}
