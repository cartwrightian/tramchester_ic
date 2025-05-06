package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableNormalServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.*;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.WriteableTransportData;

import java.util.Optional;

public abstract class TransportEntityDefaultFactory  implements TransportEntityFactory  {

    @Override
    final public MutableAgency createAgency(DataSourceID dataSourceID, AgencyData agencyData) {
        return new MutableAgency(dataSourceID, agencyData.getId(), agencyData.getName());
    }

    @Override
    final public MutableAgency createUnknownAgency(DataSourceID dataSourceID, IdFor<Agency> agencyId) {
        return new MutableAgency(dataSourceID, agencyId, "UNKNOWN");
    }

    @Override
    final public MutableService createService(IdFor<Service> serviceId) {
        return new MutableService(serviceId, getDataSourceId());
    }

    @Override
    final public RouteStation createRouteStation(Station station, Route route) {
        return new RouteStation(station, route);
    }

    @Override
    final public StopCall createPlatformStopCall(final Trip trip, final Platform platform, final Station station, final StopTimeData stopTimeData) {
        return new PlatformStopCall(platform, station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
            stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
    }

    @Override
    final public StopCall createNoPlatformStopCall(final Trip trip, final Station station, final StopTimeData stopTimeData) {
        return new NoPlatformStopCall(station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
                stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
    }

    @Override
    final public MutableNormalServiceCalendar createServiceCalendar(CalendarData calendarData) {
        return new MutableNormalServiceCalendar(calendarData);
    }

    @Override
    public abstract DataSourceID getDataSourceId();

    @Override
    public abstract MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, MutableAgency agency);

    @Override
    public abstract GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId);

    @Override
    public abstract IdFor<Station> formStationId(StopData stopData);

    @Override
    public abstract IdFor<Station> formStationId(StopTimeData stopTimeData);

    @Override
    public abstract Optional<MutablePlatform> maybeCreatePlatform(StopData stopData, Station station);

    @Override
    public abstract IdFor<Platform> getPlatformId(StopTimeData stopTimeData, Station station);

    @Override
    public abstract void logDiagnostics(WriteableTransportData writeableTransportData);

    @Override
    public abstract MutableTrip createTrip(TripData tripData, MutableService service, Route route, TransportMode transportMode);

    @Override
    public abstract MutableStation createStation(IdFor<Station> stationId, StopData stopData);

}
