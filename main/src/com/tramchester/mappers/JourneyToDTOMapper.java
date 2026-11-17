package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.ChangeLocation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;
import com.tramchester.repository.NeighboursRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class JourneyToDTOMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyToDTOMapper.class);
    private final StageDTOFactory stageFactory;
    private final DTOFactory stationDTOFactory;
    private final JourneyDTODuplicateFilter duplicateFilter;
    private final TramchesterConfig config;
    private final NeighboursRepository neighboursRepository;

    @Inject
    public JourneyToDTOMapper(StageDTOFactory stageFactory, DTOFactory DTOFactory, JourneyDTODuplicateFilter duplicateFilter,
                              TramchesterConfig config, NeighboursRepository neighboursRepository) {
        this.stageFactory = stageFactory;
        this.stationDTOFactory = DTOFactory;
        this.duplicateFilter = duplicateFilter;
        this.config = config;
        this.neighboursRepository = neighboursRepository;
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

        TravelAction previousAction = TravelAction.None;
        TransportMode previousMode = TransportMode.NotSet;
        for(final TransportStage<?,?> rawStage : rawJourneyStages) {
            final TravelAction travelAction = decideTravelAction(stages, rawStage);
            final String actionText = TravelAction.decideActionText(travelAction, rawStage.getTransportMode(), previousAction, previousMode);
            final SimpleStageDTO stageDTO = stageFactory.build(rawStage, travelAction, actionText, queryDate);
            stages.add(stageDTO);
            previousAction = travelAction;
            previousMode = rawStage.getTransportMode();
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
        return switch (rawStage.getTransportMode()) {
            case Tram, Bus, RailReplacementBus, Train, Ferry, Subway, TramReplacementBus -> decideActionForStations(stages);
            case Walk -> decideWalkingAction(rawStage);
            case Connect -> TravelAction.ConnectTo;
            case Ship, NotSet, Unknown -> throw new RuntimeException("Not defined for " + rawStage.getTransportMode());
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

    public JourneyPlanRepresentation createJourneyPlan(final Set<Journey> journeys, final boolean diagnostics, TramDate queryTramDate,
                                                       final boolean diagReceived, final JourneyDiagnostics journeyDiagnostics) {
        // duplicates where same path and timings, just different change points
        final Set<JourneyDTO> journeyDTOS = journeys.stream().
                map(journey -> createJourneyDTO(journey, queryTramDate)).
                collect(Collectors.toSet());

        return createJourneyPlan(journeyDTOS, diagnostics, diagReceived, journeyDiagnostics);

    }

    public JourneyPlanRepresentation createJourneyPlan(final Set<JourneyDTO> journeyDTOS, final boolean diagnostics,
                                                       final boolean diagReceived, final JourneyDiagnostics journeyDiagnostics) {
        final Set<JourneyDTO> filtered = duplicateFilter.apply(journeyDTOS);
        final int diff = journeyDTOS.size()-filtered.size();
        if (diff == 0) {
            logger.info(format("No filtering, returning %s journeys", journeyDTOS.size()));
        } else {
            logger.info(format("Filtered out %s of %s journeys", diff, journeyDTOS.size()));
        }

        // live data modes
        final ImmutableEnumSet<TransportMode> enabledLiveData = config.getLiveDataModes();

        final ImmutableEnumSet<TransportMode> beginNeighbourLiveDataModes;
        if (neighboursRepository.isEnabled()) {
            beginNeighbourLiveDataModes= journeyDTOS.stream().
                    map(JourneyDTO::getBegin).
                    filter(locationRefDTO -> locationRefDTO.getLocationType() == LocationType.Station).
                    map(locationRefDTO -> Station.createId(locationRefDTO.getId())).
                    flatMap(stationId -> neighboursRepository.getNeighboursFor(stationId).stream()).
                    flatMap(station -> station.getTransportModes().stream()).
                    filter(enabledLiveData::contains).
                    collect(ImmutableEnumSet.collect(TransportMode.class));
            logger.info("Live data modes for neighbours " + beginNeighbourLiveDataModes);
        } else {
            beginNeighbourLiveDataModes = ImmutableEnumSet.noneOf(TransportMode.class);
        }

        final ImmutableEnumSet<TransportMode> beginLiveDataModes = journeyDTOS.stream().
                flatMap(journeyDTO -> journeyDTO.getBegin().getTransportModes().stream()).
                filter(enabledLiveData::contains).
                collect(ImmutableEnumSet.collect(TransportMode.class));

        logger.info("Live data modes for begin " + beginLiveDataModes);

        final ImmutableEnumSet<TransportMode> liveDataModes = beginLiveDataModes.with(beginNeighbourLiveDataModes);

        // TODO From FE code, no longer needed?
         //  if (place != null) {
        //        locationType = place.locationType;
        //        if (startLocationType===locationType) {
        //            modes = app.selectedModes;
        //        } else {
        //            modes = app.startStop.transportModes;
        //        }
        //    } else {
        //        // if no location available use all selected modes
        //        locationType = startLocationType;
        //        modes = app.selectedModes;
        //    }

        // all change stations

        final Stream<LocationRefDTO> changeStationStream = journeyDTOS.stream().
                flatMap(journeyDTO -> journeyDTO.getChangeStations().stream());

        final Stream<LocationRefDTO> destinationStream = journeyDTOS.stream().
                map(JourneyDTO::getDestination);

        final Set<IdForDTO> allChangeStations = Stream.concat(changeStationStream, destinationStream).
                filter(station -> station.getLocationType()==LocationType.Station).
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        // TODO likely this will only remain for the diag API, with streamed for the production one
        final JourneyPlanRepresentation planRepresentation = new JourneyPlanRepresentation(filtered,
                allChangeStations, ImmutableEnumSet.createEnumSet(liveDataModes));

        if (diagnostics) {
            if (diagReceived) {
                planRepresentation.addDiag(journeyDiagnostics);
            } else {
                logger.error("Diagnostics requested, but not received");
            }
        }

        return planRepresentation;
    }
}
