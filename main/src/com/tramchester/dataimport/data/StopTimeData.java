package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StopTimeData {

    // ignoreUnknown significant for performance, jackson buffers unknown fields otherwise

    // trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type

    private final String tripId;
    private final String arrivalTime ;
    private final String departureTime;
    private final String stopId;
    private final int stopSequence;
    private final String pickupType;
    private final String dropOffType;

    // when dealing with millions of rows parsing of times became a bottleneck, so cache results
    private TramTime parsedArrivalTime = null;
    private TramTime parsedDepartureTime = null;

    @JsonCreator
    private StopTimeData(@JsonProperty("trip_id") final String tripId,
                         @JsonProperty("arrival_time") final String arrivalTime,
                         @JsonProperty("departure_time") final String departureTime,
                         @JsonProperty("stop_id") final String stopId,
                         @JsonProperty("stop_sequence") final int stopSequence,
                         @JsonProperty("pickup_type") final String pickupType,
                         @JsonProperty("drop_off_type") final String dropOffType) {

        this.tripId = tripId;
        this.stopId = stopId;

        this.arrivalTime = padIfNeeded(arrivalTime);
        this.departureTime = padIfNeeded(departureTime);
        this.stopSequence = stopSequence;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;

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
            parsedArrivalTime = TramTime.parse(arrivalTime);
        }
        return parsedArrivalTime;
    }

    public TramTime getDepartureTime() {
        if (parsedDepartureTime==null) {
            parsedDepartureTime = TramTime.parse(departureTime);
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
