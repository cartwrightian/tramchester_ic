package com.tramchester.graph;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;

public enum GraphPropertyKey {
    STATION_ID("station_id"),
    PLATFORM_ID("platform_id"),
    PLATFORM_NUMBER("platform_number"),
    ROUTE_STATION_ID("route_station_id"),
    TRIP_ID("trip_id"),
    TRIP_ID_LIST("trip_id_list"),
    ROUTE_ID("route_id"),
    SERVICE_ID("service_id"),
    TRANSPORT_MODE("transport_mode"),
    TRANSPORT_MODES("transport_modes"),
    AREA_ID("area_id"),
    STATION_GROUP_ID("stationgroup_id"),

    COST("cost"),
    HOUR("hour"),
    TIME("time"),
    DAY_OFFSET("day_offset"),
    TOWARDS_STATION_ID("towards_id"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    MAX_EASTING("max_easting"),
    MAX_NORTHING("max_northing"),
    MIN_EASTING("min_easting"),
    MIN_NORTHING("min_northing"),
    WALK_ID("walk_id"),
    STOP_SEQ_NUM("stop_seq_number"),
    SOURCE_NAME_PROP("source_name"),
    START_DATE("start_date"),
    END_DATE("end_date"),
    START_TIME("start_time"),
    END_TIME("end_time"),
    ALL_DAY("all_day");

    private final String text;

    GraphPropertyKey(String text) {
        this.text = text;
    }

    public static <C extends CoreDomain> GraphPropertyKey getFor(Class<C> klass) {
        if (klass.equals(Station.class)) {
            return STATION_ID;
        }
        if (klass.equals(Platform.class)) {
            return PLATFORM_ID;
        }
        if (klass.equals(RouteStation.class)) {
            return ROUTE_STATION_ID;
        }
        if (klass.equals(Trip.class)) {
            return TRIP_ID;
        }
        if (klass.equals(Route.class)) {
            return ROUTE_ID;
        }
        if (klass.equals(Service.class)) {
            return SERVICE_ID;
        }
        if (klass.equals(NPTGLocality.class)) {
            return AREA_ID;
        }
        if (klass.equals(StationLocalityGroup.class)) {
            return STATION_GROUP_ID;
        }
        throw new RuntimeException("Missing key for type" + klass);
    }

    public String getText() {
        return text;
    }

}
