package com.tramchester.graph;

public enum GraphPropertyKey {
    STATION_ID("station_id"),
    PLATFORM_ID("platform_id"),
    PLATFORM_NUMBER("platform_number"),
    ROUTE_STATION_ID("route_station_id"),
    TRIP_ID("trip_id"),
    ROUTE_ID("route_id"),
    SERVICE_ID("service_id"),
    TRANSPORT_MODE("transport_mode"),
    TRANSPORT_MODES("transport_modes"),
    AREA_ID("area_id"),

    COST("cost"),
    MAX_COST("max_cost"),
    HOUR("hour"),
    TIME("time"),
    DAY_OFFSET("day_offset"),
    TOWARDS_STATION_ID("towards_id"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    WALK_ID("walk_id"),
    STOP_SEQ_NUM("stop_seq_number"),
    SOURCE_NAME_PROP("source_name"),
    START_DATE("start_date"),
    END_DATE("end_date");

    private final String text;

    GraphPropertyKey(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
