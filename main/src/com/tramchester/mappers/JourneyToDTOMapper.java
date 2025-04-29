package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Journey;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.ChangeLocation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.ChangeStationRefWithPosition;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.SimpleStageDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@LazySingleton
public class JourneyToDTOMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyToDTOMapper.class);
    private final StageDTOFactory stageFactory;
    private final DTOFactory stationDTOFactory;

    @Inject
    public JourneyToDTOMapper(StageDTOFactory stageFactory, DTOFactory DTOFactory) {
        this.stageFactory = stageFactory;
        this.stationDTOFactory = DTOFactory;
    }

    public JourneyDTO createJourneyDTO(final Journey journey, final TramDate queryDate) {
        logger.info("Mapping journey with " + journey.getStages().size() + " stages for " + queryDate);

        final List<SimpleStageDTO> stages = new ArrayList<>();

        final List<TransportStage<?,?>> rawJourneyStages = journey.getStages();
        if (rawJourneyStages.isEmpty()) {
            final String msg = "Journey has no stages " + journey;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        final TramTime queryTime = journey.getQueryTime();

        for(final TransportStage<?,?> rawStage : rawJourneyStages) {
            final TravelAction action = decideTravelAction(stages, rawStage);
            final SimpleStageDTO stageDTO = stageFactory.build(rawStage, action, queryDate);
            stages.add(stageDTO);
        }

        final LocationRefWithPosition begin = stationDTOFactory.createLocationRefWithPosition(journey.getBeginning());

        final LocationRefWithPosition destination = stationDTOFactory.createLocationRefWithPosition(journey.getDestination());

        final List<ChangeStationRefWithPosition> changeStations = toChangeStationRefWithPosition(journey.getChangeStations());

        final List<LocationRefWithPosition> path = toLocationRefWithPosition(journey.getPath());

        final LocalDate date = queryDate.toLocalDate();

        return new JourneyDTO(begin, destination, stages,
                journey.getArrivalTime().toDate(date), journey.getDepartTime().toDate(date),
                changeStations, queryTime,
                path, date, journey.getJourneyIndex());
    }

    private List<ChangeStationRefWithPosition> toChangeStationRefWithPosition(List<ChangeLocation<?>> changeStations) {
        return changeStations.stream().
                map(changeLocation -> new ChangeStationRefWithPosition(changeLocation.location(), changeLocation.fromMode())).toList();
    }

    private List<LocationRefWithPosition> toLocationRefWithPosition(final List<Location<?>> locations) {
        return locations.stream().map(stationDTOFactory::createLocationRefWithPosition).toList();
    }

    private TravelAction decideTravelAction(final List<SimpleStageDTO> stages, final TransportStage<?,?> rawStage) {
        return switch (rawStage.getMode()) {
            case Tram, Bus, RailReplacementBus, Train, Ferry, Subway -> decideActionForStations(stages);
            case Walk -> decideWalkingAction(rawStage);
            case Connect -> TravelAction.ConnectTo;
            default -> throw new RuntimeException("Not defined for " + rawStage.getMode());
        };
    }

    private TravelAction decideWalkingAction(TransportStage<?,?> rawStage) {
        WalkingStage<?,?> walkingStage = (WalkingStage<?,?>) rawStage;
        return walkingStage.getTowardsMyLocation() ? TravelAction.WalkFrom : TravelAction.WalkTo;
    }

    private TravelAction decideActionForStations(final List<SimpleStageDTO> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        final SimpleStageDTO previousStage = stagesSoFar.getLast();
        final TransportMode previousMode = previousStage.getMode();
        if ((previousMode ==TransportMode.Walk) || previousMode ==TransportMode.Connect) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }
}
