package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.data.TripData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.naptan.NaptanStopType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TransportEntityFactoryForTFGM extends TransportEntityFactory {

    private static final String METROLINK_ID_PREFIX = "9400ZZ";
    private static final String METROLINK_NAME_POSTFIX = "(Manchester Metrolink)";

    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactoryForTFGM.class);

    private final NaptanRepository naptanRepository;
    private final boolean naptanEnabled;

    private final Duration minChangeDuration = Duration.ofMinutes(MutableStation.DEFAULT_MIN_CHANGE_TIME);

    private final Map<String, IdFor<Station>> stopIdToStationId;
    private final Map<String, String> originalCodeForStop; // stopId -> full stopCode with platform suffix

    public TransportEntityFactoryForTFGM(NaptanRepository naptanRepository) {
        super();
        this.naptanRepository = naptanRepository;
        this.naptanEnabled = naptanRepository.isEnabled();
        this.stopIdToStationId = new HashMap<>();
        this.originalCodeForStop = new HashMap<>();
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.tfgm;
    }

    @Override
    public MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, MutableAgency agency) {

        IdFor<Route> routeId = getCorrectIdFor(routeData);

        String routeName = routeData.getLongName();
        return new MutableRoute(routeId, routeData.getShortName().trim(), routeName, agency,
                GTFSTransportationType.toTransportMode(routeType));
    }

    private IdFor<Route> getCorrectIdFor(RouteData routeData) {
        final String idText = routeData.getId();
        return Route.createId(idText);
    }

    @Override
    public MutableStation createStation(final IdFor<Station> stationId, final StopData stopData) {

        final boolean isMetrolink = isMetrolinkTram(stopData);
        final String stationCode = stopData.getCode();
        final boolean hasNaptan = hasNaptan(stationCode);
        final NaptanRecord naptanRecord = hasNaptan ? naptanRepository.getForActo(stationId) : null;

        final boolean isInterchange;
        final IdFor<NPTGLocality> areaId;
        final LatLong latLong;
        final GridPosition gridPosition;
        if (hasNaptan) {
                isInterchange = NaptanStopType.isInterchange(naptanRecord.getStopType());
                areaId = naptanRecord.getLocalityId();
                gridPosition = naptanRecord.getGridPosition();
                latLong = naptanRecord.getLatLong();
        } else {
            isInterchange = false;
            areaId = NPTGLocality.InvalidId();
            latLong = stopData.getLatLong();
            gridPosition = CoordinateTransforms.getGridPosition(latLong);
        }

        final String cleanedName = cleanStationName(stopData); // remove embedded quotes

        final String stationName;
        if (isMetrolink) {
            stationName = removeMetrolinkPostfix(cleanedName);
        } else {
            if (hasNaptan) {
                stationName = naptanRecord.getDisplayName();
            } else {
                stationName = cleanedName;
            }
        }

        return new MutableStation(stationId, areaId, workAroundName(stationName), latLong, gridPosition,
                getDataSourceId(), isInterchange, minChangeDuration);
    }

    boolean hasNaptan(String stationCode) {
        if (naptanEnabled) {
            return naptanRepository.containsActo(Station.createId(stationCode));
        }
        return false;
    }

    @Override
    public Optional<MutablePlatform> maybeCreatePlatform(StopData stopData, Station station) {

        // TODO better way to do this
        if (!isMetrolinkTram(stopData)) {
            return Optional.empty();
        }

        final String stopCode = stopData.getCode();
        IdFor<Station> stationId = stopIdToStationId.get(stopData.getId());

        PlatformId platformId = createPlatformId(stationId, stopCode);

        final String platformNumber = platformId.getNumber();

        IdFor<NPTGLocality> areaId = NPTGLocality.InvalidId();
        LatLong latLong = stopData.getLatLong();
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(latLong);

        if (naptanEnabled) {
            if (naptanRepository.containsActo(platformId)) {
                NaptanRecord naptanData = naptanRepository.getForActo(platformId);

                areaId = naptanData.getLocalityId();
                gridPosition = naptanData.getGridPosition();
                latLong = naptanData.getLatLong();
            }

            // TODO Add logging if there is a significant diff in position data?
        }

        String platformName = removeMetrolinkPostfix(cleanStationName(stopData));

        final MutablePlatform platform = new MutablePlatform(platformId, station, platformName,
                getDataSourceId(), platformNumber, areaId, latLong, gridPosition, station.isMarkedInterchange());
        return Optional.of(platform);

    }

    @Override
    public MutableTrip createTrip(TripData tripData, MutableService service, Route route, TransportMode transportMode) {
        final String headSign = removeMetrolinkPostfix(tripData.getHeadsign());
        final MutableTrip trip = new MutableTrip(tripData.getTripId(), headSign, service, route, transportMode);
        service.addTrip(trip);
        return trip;
    }

    @Override
    public IdFor<Platform> getPlatformId(StopTimeData stopTimeData, Station station) {
        String originalCode = originalCodeForStop.get(stopTimeData.getStopId()); // contains the platform suffix
        return createPlatformId(station.getId(), originalCode);
    }

    public static PlatformId createPlatformId(IdFor<Station> stationId, final String fullCodeWithPlatformSuffix) {

        String remaining = StringIdFor.removeIdFrom(fullCodeWithPlatformSuffix, stationId);
        if (remaining.isEmpty()) {
            throw new RuntimeException("Resulting platform number is empty for " + stationId + " and " + fullCodeWithPlatformSuffix);
        }

        return PlatformId.createId(stationId, remaining);
    }

    private String cleanStationName(final StopData stopData) {
        String text = stopData.getName();
        text = text.replace("\"", "").trim();
        return text;
    }

    @NotNull
    private static String removeMetrolinkPostfix(final String text) {
        if (text.endsWith(METROLINK_NAME_POSTFIX)) {
            return text.replace(METROLINK_NAME_POSTFIX, "").trim();
        } else {
            return text;
        }
    }

    @Override
    public IdFor<Station> formStationId(StopData stopData) {
        String stopId = stopData.getId();
        String stopCode = stopData.getCode();

        IdFor<Station> stationId = getStationIdFor(stopCode);
        stopIdToStationId.put(stopId, stationId);
        originalCodeForStop.put(stopId, stopCode);
        return stationId;
    }

    @Override
    public IdFor<Station> formStationId(StopTimeData stopTimeData) {
        return stopIdToStationId.get(stopTimeData.getStopId());
    }

    @NotNull
    public static IdFor<Station> getStationIdFor(String stationCode) {
        if (stationCode.startsWith(METROLINK_ID_PREFIX)) {
            // metrolink platform ids include platform as final digit, remove to give id of station itself
            int index = stationCode.length()-1;
            return Station.createId(stationCode.substring(0,index));
        }
        return Station.createId(stationCode);
    }

    private boolean isMetrolinkTram(StopData stopData) {
        return stopData.getCode().startsWith(METROLINK_ID_PREFIX);
    }

    // TODO Consolidate handling of various TFGM mappings and monitor if still needed
    // spelt different ways within data
    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
    }

    @Override
    public GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId) {
        GTFSTransportationType routeType = routeData.getRouteType();
        boolean isMetrolink = Agency.IsMetrolink(agencyId);

        // NOTE: this data issue has been reported to TFGM
        if (isMetrolink && routeType!=GTFSTransportationType.tram) {
            logger.error("METROLINK Agency seen with transport type " + routeType.name() + " for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.tram.name() + " for " + routeData);
            return GTFSTransportationType.tram;
        }

        // NOTE: this data issue has been reported to TFGM
        if ( (routeType==GTFSTransportationType.tram) && (!isMetrolink) ) {
            logger.error("Tram transport type seen for non-metrolink agency for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.bus.name() + " for " + routeData);
            return GTFSTransportationType.bus;
        }

        return routeType;

    }

}
