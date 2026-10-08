package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.presentation.LatLong;

import java.util.Objects;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StopData {

    //@JsonProperty("stop_id")
    private final String id;
    //@JsonProperty("stop_code")
    private final String code;
    //@JsonProperty("stop_lat")
    private final double latitude;
    //@JsonProperty("stop_lon")
    private final double longitude;
    //@JsonProperty("stop_name")
    private final String name;

    @JsonCreator
    public StopData(@JsonProperty("stop_id") String id,
            @JsonProperty("stop_code") String code,
            @JsonProperty("stop_lat") double latitude,
            @JsonProperty("stop_lon") double longitude,
            @JsonProperty("stop_name") String name) {
        this.id = id;
        this.code = code;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

//    // deserialization
//    public StopData() {
//    }

    public String getId() {
        return id;
    }

    /***
     * the underlying GTFS id for the stop
     * @return the gtfs code
     */
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public LatLong getLatLong() {
        if (latitude==0 || longitude==0) {
            return LatLong.Invalid;
        }
        return new LatLong(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopData stopData = (StopData) o;
        return Objects.equals(id, stopData.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StopData{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

}
