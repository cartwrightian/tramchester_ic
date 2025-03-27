package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.DTO.factory.LocationDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.mappers.RecentJourneysToLocations;
import com.tramchester.repository.*;
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
@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyLocationsResource extends UsesRecentCookie implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyLocationsResource.class);

    private final StationRepositoryPublic stationRepository;
    private final StationGroupsRepository stationGroupsRepository;
    private final DataSourceRepository dataSourceRepository;
    private final StationLocations stationLocations;
    private final DTOFactory DTOFactory;
    private final TramchesterConfig config;
    private final TransportModeRepository transportModeRepository;
    private final RecentJourneysToLocations recentJourneysToLocations;

    @Inject
    public JourneyLocationsResource(StationRepository stationRepository,
                                    UpdateRecentJourneys updateRecentJourneys,
                                    ProvidesNow providesNow,
                                    StationGroupsRepository stationGroupsRepository, DataSourceRepository dataSourceRepository,
                                    StationLocations stationLocations, DTOFactory DTOFactory, LocationDTOFactory locationDTOFactory,
                                    TramchesterConfig config, TransportModeRepository transportModeRepository,
                                    RecentJourneysToLocations recentJourneysToLocations) {
        super(updateRecentJourneys, providesNow);
        this.stationGroupsRepository = stationGroupsRepository;
        this.DTOFactory = DTOFactory;
        this.transportModeRepository = transportModeRepository;
        this.recentJourneysToLocations = recentJourneysToLocations;
        logger.info("created");
        this.stationRepository = stationRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.stationLocations = stationLocations;
        this.config = config;
    }

    @GET
    @Timed
    @Path("/mode/{mode}")
    @Operation(description = "Get all locations for transport mode")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS, isPrivate = false)
    public Response getByMode(@PathParam("mode") String rawMode, @Context Request request) {
        logger.info("Get locations for transport mode: " + rawMode);

        try {
            final TransportMode mode = TransportMode.valueOf(rawMode);

            final ZonedDateTime modTime = dataSourceRepository.getNewestModTimeFor(mode);
            final Date date = Date.from(modTime.toInstant());

            Response.ResponseBuilder builder = request.evaluatePreconditions(date);

            if (builder==null) {
                final List<LocationRefDTO> results = getLocationsFor(mode);
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

    @NotNull
    private List<LocationRefDTO> getLocationsFor(TransportMode mode) {
        final Set<? extends Location<?>> matching;
        if (mode==TransportMode.Bus) {
            if (!stationGroupsRepository.isEnabled()) {
                return Collections.emptyList();
            }
            logger.info("Get list of station groups for " + mode);
            matching = stationGroupsRepository.getStationGroupsFor(mode);
        } else {
            logger.info("Get list of stations for " + mode);
            matching = stationRepository.getStationsServing(mode);
        }

        final List<LocationRefDTO> results = toLocationRefDTOList(matching);
        if (results.isEmpty()) {
            logger.warn("No stations found for " + mode.name());
        } else {
            logger.info("Returning " + results.size() + " stations for mode " + mode);
        }
        return results;

    }

    @GET
    @Timed
    @Path("/near/{mode}")
    @Operation(description = "Get locations close to a given lat/lon")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(noCache = true)
    public Response getNearWithMode(@PathParam("mode") String rawMode, @QueryParam("lat") double lat, @QueryParam("lon") double lon) {
        MarginInMeters margin = MarginInMeters.ofKM(config.getNearestStopRangeKM());
        logger.info(format("Get locations within %s of %s,%s and mode %s", margin, lat, lon, rawMode));

        TransportMode mode = TransportMode.valueOf(rawMode);

        LatLong latLong = new LatLong(lat,lon);

        MyLocation location = new MyLocation(latLong);

        List<? extends Location<?>> nearestLocations = getNearestLocations(location, margin, mode);

        List<LocationRefDTO> results = toLocationRefDTOList(nearestLocations);

        return Response.ok(results).build();
    }

    private List<? extends Location<?>> getNearestLocations(MyLocation origin, MarginInMeters margin, TransportMode mode) {
        EnumSet<TransportMode> modes = EnumSet.of(mode);

        List<Station> stations = stationLocations.nearestStationsSorted(origin, config.getNumOfNearestStopsToOffer(), margin, modes);

        if (mode==TransportMode.Bus) {
            // convert to localities
            IdSet<StationLocalityGroup> localityIds = stations.stream().
                    map(Location::getLocalityId).
                    map(StationLocalityGroup::createId).
                    filter(IdFor::isValid).
                    collect(IdSet.idCollector());
            logger.info("Convert nearest stations to localities for " + mode);
            return localityIds.stream().map(stationGroupsRepository::getStationGroup).toList();
        } else {
            return stations;
        }
    }

    @GET
    @Timed
    @Path("/recent")
    @Operation(description = "Get recent locations based on supplied cookie")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(noCache = true)
    public Response getRecent(@CookieParam(TRAMCHESTER_RECENT) Cookie cookie, @QueryParam("modes") String rawModes) {
        logger.info(format("Get recent locations for cookie %s and modes", cookie, rawModes));

        final EnumSet<TransportMode> modes;
        if (rawModes!=null) {
            modes = TransportMode.parseCSV(rawModes);
        } else {
            modes = transportModeRepository.getModes();
        }

        RecentJourneys recentJourneys = recentFromCookie(cookie);

        logger.info("Recent Journeys are " + recentJourneys);

        Set<Location<?>> recent = recentJourneysToLocations.from(recentJourneys, modes);

        List<LocationRefDTO> results = toLocationRefDTOList(recent);

        return Response.ok(results).build();
    }

    @NotNull
    private List<LocationRefDTO> toLocationRefDTOList(Collection<? extends Location<?>> stations) {
        return stations.stream().
                map(DTOFactory::createLocationRefDTO).
                // sort server side is here as an optimisation for front end sorting time
                sorted(Comparator.comparing(dto -> dto.getName().toLowerCase())).
                collect(Collectors.toList());
    }


}
