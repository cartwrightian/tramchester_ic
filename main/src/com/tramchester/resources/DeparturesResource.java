package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.query.DeparturesQueryDTO;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.DepartureListDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.repository.ProvidesNotes;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;
import com.tramchester.repository.LocationRepository;
import com.tramchester.repository.StationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Path("/departures")
@Produces(MediaType.APPLICATION_JSON)
public class DeparturesResource extends TransportResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesResource.class);

    private final LocationRepository locationRepository;
    private final StationRepository stationRepository;
    private final DeparturesMapper departuresMapper;
    private final DeparturesRepository departuresRepository;
    private final ProvidesNotes providesNotes;
    private final TramchesterConfig config;

    @Inject
    public DeparturesResource(LocationRepository locationRepository, StationRepository stationRepository,
                              DeparturesMapper departuresMapper, DeparturesRepository departuresRepository,
                              ProvidesTramNotes providesNotes,
                              ProvidesNow providesNow, TramchesterConfig config) {
        super(providesNow);
        this.locationRepository = locationRepository;
        this.stationRepository = stationRepository;
        this.departuresMapper = departuresMapper;
        this.departuresRepository = departuresRepository;
        this.providesNotes = providesNotes;
        this.config = config;
        logger.info("created");
    }

    @POST
    @Timed
    @Path("/location")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Get departures for a location")
    @ApiResponse(content = @Content(schema = @Schema(implementation = DepartureListDTO.class)))
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestDepartures(DeparturesQueryDTO departuresQuery) {

        if (departuresQuery.getLocationType()==null || departuresQuery.getLocationId()==null) {
            logger.error("Cannot process departure query: " + departuresQuery);
            return Response.serverError().build();
        }

        logger.info("Get departures for " + departuresQuery);

        final Location<?> location = locationRepository.getLocation(departuresQuery.getLocationType(),
                departuresQuery.getLocationId());

        // assume today, no live data otherwise
        final LocalDateTime dateTime = providesNow.getDateTime();
        final TramDate queryDate = TramDate.from(dateTime);

        final TramTime queryTime;
        if (departuresQuery.hasValidTime()) {
            queryTime = TramTime.ofHourMins(departuresQuery.getTime());
        } else {
            queryTime = providesNow.getNowHourMins();
        }

        Set<IdForDTO> notesFor = departuresQuery.getNotesFor() == null ? Collections.emptySet() : departuresQuery.getNotesFor();

        EnumSet<TransportMode> modes = departuresQuery.getModes();
        if (modes.isEmpty()) {
            logger.warn("modes not supplied, fall back to all configured modes");
            modes = config.getTransportModes();
        }

        final List<UpcomingDeparture> dueTrams = departuresRepository.getDueForLocation(location, dateTime.toLocalDate(), queryTime, modes);
        if (dueTrams.isEmpty()) {
            logger.warn("Departures list empty for " + location.getId() + " at " + queryTime);
        }

        final SortedSet<DepartureDTO> departs;
        departs = mapToDTOs(departuresQuery, dueTrams);

        final List<Note> notes = getNotes(notesFor, dueTrams, queryDate, queryTime, location);

        return Response.ok(new DepartureListDTO(departs, notes)).build();
    }

    private @NotNull SortedSet<DepartureDTO> mapToDTOs(final DeparturesQueryDTO departuresQuery, final List<UpcomingDeparture> dueTrams) {

        if (departuresQuery.hasJourneys()) {
            List<JourneyDTO> journeys = departuresQuery.getJourneys();
            final IdSet<Station> journeyFirstDestinationIds = getFirstDestIds(journeys);
            final IdFor<Station> finalStationId = getFinalStationId(journeys);
            //if (!journeyFirstDestinationIds.isEmpty()) {
                logger.info("Fetching due trams and checking for initial destinations " + journeyFirstDestinationIds +
                        " and final station " + finalStationId);
                return new TreeSet<>(departuresMapper.mapToDTO(dueTrams, providesNow.getDateTime(), journeyFirstDestinationIds, finalStationId));
            //}
        } else {

            logger.info("Mapping due trams, not checking for tram destinations");
            return new TreeSet<>(departuresMapper.mapToDTO(dueTrams, providesNow.getDateTime()));
        }
    }

    private IdSet<Station> getFirstDestIds(final List<JourneyDTO> journeys) {
        return journeys.stream().
                flatMap(journeyDTO -> journeyDTO.getChangeStations().stream()).
                filter(ref -> ref.getLocationType()==LocationType.Station).
                map(LocationRefDTO::getId).
                map(id -> Station.createId(id.getActualId())).collect(IdSet.idCollector());
    }

    private IdFor<Station> getFinalStationId(final List<JourneyDTO> journeyDTOS) {
        IdSet<Station> unique = journeyDTOS.stream().
                map(JourneyDTO::getPath).
                map(this::lastStationIn).
                map(LocationRefDTO::getId).
                map(Station::createId).
                collect(IdSet.idCollector());

        List<IdFor<Station>> stations = unique.toList();

        if (stations.isEmpty()) {
            logger.error("Could not find any final destinations from journeys " + journeyDTOS);
            return IdFor.invalid(Station.class);
        }

        if (stations.size()==1) {
            return stations.get(0);
        }

        // TODO is it ok just pick one?
        String msg = "Found multiple final stations for " + journeyDTOS;
        logger.error(msg);
        throw new RuntimeException(msg);

    }

    private LocationRefWithPosition lastStationIn(List<LocationRefWithPosition> path) {
        for (int i = path.size()-1; i >= 0; i--) {
            final LocationRefWithPosition location = path.get(i);
            if (location.getLocationType()==LocationType.Station) {
                return location;
            }
        }
        throw new RuntimeException("Failed to find a stations in " + path);
    }

    private IdSet<Station> getStationIds(final Set<IdForDTO> firstDestIds) {
        IdSet<Station> result = firstDestIds.stream().map(Station::createId).filter(stationRepository::hasStationId).collect(IdSet.idCollector());
        if (result.size()!=firstDestIds.size()) {
            logger.warn("Unable to map all firstDestIds " + firstDestIds + " to station ids, only got " + result);
        }
        return result;
    }

    @NotNull
    private List<Note> getNotes(Set<IdForDTO> notesFor, List<UpcomingDeparture> dueTrams, TramDate queryDate, TramTime queryTime, Location<?> location) {
        Set<Station> stations = getStationsToQueryForNotes(notesFor, dueTrams);
        final List<Note> notes = providesNotes.createNotesForStations(stations, queryDate, queryTime);
        if (notes.isEmpty()) {
            logger.warn("Notes empty for " + location.getId() + " at " + queryTime);
        }
        return notes;
    }

    private Set<Station> getStationsToQueryForNotes(final Set<IdForDTO> notesFor, final List<UpcomingDeparture> dueTrams) {
        if (notesFor.isEmpty()) {
            // based on the nearby departures
            logger.info("No specific stations provided, default to nearby");
            return dueTrams.stream().map(UpcomingDeparture::getDisplayLocation).collect(Collectors.toSet());
        } else {
            // TODO other location types needed?
            logger.info("Getting notes for specific stations " + notesFor);
            return notesFor.stream().
                    filter((stationId -> locationRepository.hasLocation(LocationType.Station, stationId))).
                    map(stationId -> locationRepository.getLocation(LocationType.Station, stationId)).
                    map(location -> (Station)location).
                    collect(Collectors.toSet());
        }
    }


}
