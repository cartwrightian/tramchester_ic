package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StopTimeData {

    // ignoreUnknown significant for performance, jackson buffers unknown fields otherwise

    // trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type

    // when dealing with millions of rows parsing of times became a bottleneck, so cache results
    private TramTime parsedArrivalTime = null;
    private TramTime parsedDepartureTime = null;

    @JsonProperty("trip_id")
    private String tripId;
    @JsonProperty("arrival_time")
    private String arrivalTime;
    @JsonProperty("departure_time")
    private String departureTime;
    @JsonProperty("stop_id")
    private String stopId;
    @JsonProperty("stop_sequence")
    private int stopSequence;
    @JsonProperty("pickup_type")
    private String pickupType;
    @JsonProperty("drop_off_type")
    private String dropOffType;

    public StopTimeData() {
        // faster for AfterBurner in Mapper as will use code gen, see
        // TransportDataReaderFactory and https://github.com/FasterXML/jackson-modules-base/tree/2.19/afterburner
    }

    private String padIfNeeded(final String text) {
        final int indexOfFirstDivider = text.indexOf(':');
        if (indexOfFirstDivider==2) {
            return text;
        }
        if (indexOfFirstDivider==1) {
            return "0"+text;
        }
        return text;
    }

    @Override
    public String toString() {
        return "StopTimeData{" +
                "tripId='" + tripId + '\'' +
                ", arrivalTime='" + arrivalTime + '\'' +
                ", departureTime='" + departureTime + '\'' +
                ", stopId='" + stopId + '\'' +
                ", stopSequence=" + stopSequence +
                ", pickupType='" + pickupType + '\'' +
                ", dropOffType='" + dropOffType + '\'' +
                ", parsedArrivalTime=" + parsedArrivalTime +
                ", parsedDepartureTime=" + parsedDepartureTime +
                '}';
    }

    // Significant during initial load, avoid doing this more than needed
    public String getTripId() {
        return tripId;
    }

    public TramTime getArrivalTime() {
        if (parsedArrivalTime==null) {
            parsedArrivalTime = TramTime.parse(padIfNeeded(arrivalTime));
        }
        return parsedArrivalTime;
    }

    public TramTime getDepartureTime() {
        if (parsedDepartureTime==null) {
            parsedDepartureTime = TramTime.parse(padIfNeeded(departureTime));
        }
        return parsedDepartureTime;
    }

    public String getStopId() {
        return stopId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public GTFSPickupDropoffType getPickupType() {
        return GTFSPickupDropoffType.fromString(pickupType);
    }

    public GTFSPickupDropoffType getDropOffType() {
        return GTFSPickupDropoffType.fromString(dropOffType);
    }

    public boolean isValid() {
        // avoid parse code, performance
        if (arrivalTime==null || departureTime==null) {
            return false;
        }
        if (arrivalTime.isEmpty() || departureTime.isEmpty()) {
            return false;
        }
        return getArrivalTime().isValid() && getDepartureTime().isValid();
    }
}
