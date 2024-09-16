package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.StationClosureDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.DTO.factory.LocationDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.mappers.RecentJourneysToLocations;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationRepositoryPublic;
import com.tramchester.repository.TransportModeRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@SuppressWarnings("unused")
@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource extends UsesRecentCookie implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(StationResource.class);

    private final StationRepositoryPublic stationRepository;
    private final DataSourceRepository dataSourceRepository;
    private final StationLocations stationLocations;
    private final DTOFactory DTOFactory;
    private final LocationDTOFactory locationDTOFactory;
    private final TramchesterConfig config;
    private final TransportModeRepository transportModeRepository;
    private final RecentJourneysToLocations recentJourneysToStations;

    // TODO Remove unused methods

    @Inject
    public StationResource(StationRepository stationRepository,
                           UpdateRecentJourneys updateRecentJourneys,
                           ProvidesNow providesNow,
                           DataSourceRepository dataSourceRepository, StationLocations stationLocations,
                           DTOFactory DTOFactory,
                           LocationDTOFactory locationDTOFactory, TramchesterConfig config, TransportModeRepository transportModeRepository,
                           RecentJourneysToLocations recentJourneysToStations) {
        super(updateRecentJourneys, providesNow);
        this.DTOFactory = DTOFactory;
        this.locationDTOFactory = locationDTOFactory;
        this.transportModeRepository = transportModeRepository;
        this.recentJourneysToStations = recentJourneysToStations;
        this.stationRepository = stationRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.stationLocations = stationLocations;
        this.config = config;
    }

    @GET
    @Timed
    @Path("/{id}")
    @Operation(description = "Get station by id")
    @ApiResponse(content = @Content(schema = @Schema(implementation = LocationDTO.class)))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response get(@PathParam("id") String text) {
        logger.info("Get station by id: " + text);
        
        IdFor<Station> id = Station.createId(text);
        guardForStationNotExisting(stationRepository, id);

        final LocationDTO locationDTO = locationDTOFactory.createLocationDTO(stationRepository.getStationById(id));
        return Response.ok(locationDTO).build();
    }

    @GET
    @Timed
    @Path("/mode/{mode}")
    @Operation(description = "Get all stations for transport mode")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS, isPrivate = false)
    public Response getByMode(@PathParam("mode") String rawMode, @Context Request request) {
        logger.info("Get stations for transport mode: " + rawMode);

        try {
            final TransportMode mode = TransportMode.valueOf(rawMode);

            final ZonedDateTime dataModTime = dataSourceRepository.getNewestModTimeFor(mode);
            final Date date = Date.from(dataModTime.toInstant());

            final Response.ResponseBuilder builder = request.evaluatePreconditions(date);

            if (builder==null) {
                final Set<Station> matching = stationRepository.getStationsServing(mode);
                final List<LocationRefDTO> results = toLocationDTOs(matching);
                if (results.isEmpty()) {
                    logger.warn("No stations found for " + mode.name());
                } else {
                    logger.info("Returning " + results.size() + " stations for mode " + mode);
                }
                return Response.ok(results).lastModified(date).build();
            } else {
                logger.info("Returning Not Modified for stations mode " + mode);
                return builder.build();
            }
        }
        catch(IllegalArgumentException missing) {
            logger.warn("Unable to match transport mode string " + rawMode, missing);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Timed
    @Path("/all")
    @Operation(description = "Get all stations")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS, isPrivate = false)
    public Response getByMode(@Context Request request) {
        logger.info("Get all stations");

        List<LocationDTO> results = stationRepository.getActiveStationStream().
                map(locationDTOFactory::createLocationDTO).
                collect(Collectors.toList());
        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/near")
    @Operation(description = "Get stations close to a given lat/lon")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(noCache = true)
    public Response getNear(@QueryParam("lat") double lat, @QueryParam("lon") double lon) {
        MarginInMeters margin = MarginInMeters.ofKM(config.getNearestStopRangeKM());
        logger.info(format("Get stations with %s of %s,%s", margin, lat, lon));

        EnumSet<TransportMode> modes = transportModeRepository.getModes();

        LatLong latLong = new LatLong(lat,lon);

        MyLocation location = new MyLocation(latLong);

        List<Station> nearestStations = stationLocations.nearestStationsSorted(location,
                config.getNumOfNearestStopsToOffer(), margin, modes);

        List<LocationRefDTO> results = toLocationDTOs(nearestStations);

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/near/{mode}")
    @Operation(description = "Get stations close to a given lat/lon")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(noCache = true)
    public Response getNearWithMode(@PathParam("mode") String rawMode, @QueryParam("lat") double lat, @QueryParam("lon") double lon) {
        MarginInMeters margin = MarginInMeters.ofKM(config.getNearestStopRangeKM());
        logger.info(format("Get stations with %s of %s,%s and mode %s", margin, lat, lon, rawMode));

        TransportMode mode = TransportMode.valueOf(rawMode);

        EnumSet<TransportMode> modes = EnumSet.of(mode);

        LatLong latLong = new LatLong(lat,lon);

        MyLocation location = new MyLocation(latLong);

        List<Station> nearestStations = stationLocations.nearestStationsSorted(location,
                config.getNumOfNearestStopsToOffer(), margin, modes);

        List<LocationRefDTO> results = toLocationDTOs(nearestStations);

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/recent")
    @Operation(description = "Get recent stations based on supplied cookie")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(noCache = true)
    public Response getRecent(@CookieParam(TRAMCHESTER_RECENT) Cookie cookie, @QueryParam("modes") String rawModes) {
        logger.info(format("Get recent stations for cookie %s", cookie));

        final EnumSet<TransportMode> modes;
        if (rawModes!=null) {
            modes = TransportMode.parseCSV(rawModes);
        } else {
            modes = transportModeRepository.getModes();
        }

        RecentJourneys recentJourneys = recentFromCookie(cookie);

        Set<Location<?>> recent = recentJourneysToStations.from(recentJourneys, modes);

        List<LocationRefDTO> results = toLocationDTOs(recent);

        return Response.ok(results).build();
    }

    @NotNull
    private List<LocationRefDTO> toLocationDTOs(Collection<? extends Location<?>> locations) {
        return locations.stream().
                map(DTOFactory::createLocationRefDTO).
                // sort server side is here as an optimisation for front end sorting time
                sorted(Comparator.comparing(dto -> dto.getName().toLowerCase())).
                collect(Collectors.toList());
    }

    @GET
    @Timed
    @Path("/closures")
    @Operation(description = "Get closed stations")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = StationClosureDTO.class))))
    @CacheControl(maxAge = 5, maxAgeUnit = TimeUnit.MINUTES)
    public Response getClosures() {
        Set<StationClosures> closed = getUpcomingClosuresFor(providesNow.getTramDate());

        logger.info("Get closed stations " + closed);

        List<StationClosureDTO> dtos = closed.stream().
                map(closure -> StationClosureDTO.from(closure, toRefs(closure.getStations()))).
                collect(Collectors.toList());

        logger.info("Returning " + dtos.size() + " closures");

        return Response.ok(dtos).build();

    }

    private Set<StationClosures> getUpcomingClosuresFor(final TramDate date) {

        return config.getGTFSDataSource().stream().
                flatMap(dataSourceRepository -> dataSourceRepository.getStationClosures().stream()).
                filter(closure -> date.isBefore(closure.getDateRange().getEndDate()) || date.equals(closure.getDateRange().getEndDate())).
                collect(Collectors.toSet());
    }


    private List<LocationRefDTO> toRefs(IdSet<Station> closedStations) {
        return closedStations.stream().
                map(stationRepository::getStationById).
                map(DTOFactory::createLocationRefDTO).
                collect(Collectors.toList());
    }

}
