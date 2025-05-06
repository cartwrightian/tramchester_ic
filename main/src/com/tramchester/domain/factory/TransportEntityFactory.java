package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableNormalServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.WriteableTransportData;

import java.util.Optional;

public interface TransportEntityFactory {

    DataSourceID getDataSourceId();

    MutableAgency createAgency(DataSourceID dataSourceID, AgencyData agencyData) ;

    MutableAgency createUnknownAgency(DataSourceID dataSourceID, IdFor<Agency> agencyId);

    MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, MutableAgency agency);

    MutableService createService(IdFor<Service> serviceId);

    MutableTrip createTrip(TripData tripData, MutableService service, Route route, TransportMode transportMode);

    MutableStation createStation(IdFor<Station> stationId, StopData stopData);

    RouteStation createRouteStation(Station station, Route route);

    StopCall createPlatformStopCall(final Trip trip, final Platform platform, final Station station, final StopTimeData stopTimeData);

    StopCall createNoPlatformStopCall(final Trip trip, final Station station, final StopTimeData stopTimeData);

    MutableNormalServiceCalendar createServiceCalendar(CalendarData calendarData);

    GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId);

    //public IdFor<Route> createRouteId(String routeIdText);
    //IdFor<Route> createRouteId(GTFSTransportationType routeType, RouteData routeData);

    IdFor<Station> formStationId(StopData stopData);

    IdFor<Station> formStationId(StopTimeData stopTimeData);

    Optional<MutablePlatform> maybeCreatePlatform(StopData stopData, Station station) ;

    IdFor<Platform> getPlatformId(StopTimeData stopTimeData, Station station);

    void logDiagnostics(WriteableTransportData writeableTransportData);

}
