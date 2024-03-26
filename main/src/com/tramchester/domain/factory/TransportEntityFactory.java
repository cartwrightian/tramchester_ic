package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableNormalServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.*;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.WriteableTransportData;

import java.util.Optional;

public abstract class TransportEntityFactory {

//    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactory.class);

    public TransportEntityFactory() {
    }

    public abstract DataSourceID getDataSourceId();

    public MutableAgency createAgency(DataSourceID dataSourceID, AgencyData agencyData) {
        return new MutableAgency(dataSourceID, agencyData.getId(), agencyData.getName());
    }

    public MutableAgency createUnknownAgency(DataSourceID dataSourceID, IdFor<Agency> agencyId) {
        return new MutableAgency(dataSourceID, agencyId, "UNKNOWN");
    }

    public MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, MutableAgency agency) {

        final String routeIdText = routeData.getId();
        IdFor<Route> routeId = Route.createId(routeIdText);

        return new MutableRoute(routeId, routeData.getShortName().trim(), routeData.getLongName(), agency,
                GTFSTransportationType.toTransportMode(routeType));

    }

    public MutableService createService(IdFor<Service> serviceId) {
        return new MutableService(serviceId, getDataSourceId());
    }

    public MutableTrip createTrip(TripData tripData, MutableService service, Route route, TransportMode transportMode) {
        final MutableTrip trip = new MutableTrip(tripData.getTripId(), tripData.getHeadsign(), service, route, transportMode);
        service.addTrip(trip);
        return trip;
    }

    public MutableStation createStation(IdFor<Station> stationId, StopData stopData) {
        // default is not to enrich from naptan and nptg
        IdFor<NPTGLocality> areaId = NPTGLocality.InvalidId();
        GridPosition position = CoordinateTransforms.getGridPosition(stopData.getLatLong());
        return new MutableStation(stationId, areaId, stopData.getName(), stopData.getLatLong(), position, getDataSourceId(), false);
    }

    public RouteStation createRouteStation(Station station, Route route) {
        return new RouteStation(station, route);
    }

    public StopCall createPlatformStopCall(Trip trip, Platform platform, Station station, StopTimeData stopTimeData) {
        return new PlatformStopCall(platform, station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
            stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
    }

    public StopCall createNoPlatformStopCall(Trip trip, Station station, StopTimeData stopTimeData) {
        return new NoPlatformStopCall(station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
                stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
    }

    public MutableNormalServiceCalendar createServiceCalendar(CalendarData calendarData) {
        return new MutableNormalServiceCalendar(calendarData);
    }

    public GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId) {
        return routeData.getRouteType();
    }

    public IdFor<Route> createRouteId(String routeIdText) {
        return Route.createId(routeIdText);
    }

    public abstract IdFor<Station> formStationId(StopData stopData);

    public abstract IdFor<Station> formStationId(StopTimeData stopTimeData);

    public Optional<MutablePlatform> maybeCreatePlatform(StopData stopData, Station station) {
        return Optional.empty();
    }

    public abstract IdFor<Platform> getPlatformId(StopTimeData stopTimeData, Station station);

    public abstract void logDiagnostics(WriteableTransportData writeableTransportData);

}
