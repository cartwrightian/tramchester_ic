package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;

// holds exceptions to main calendar
@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalendarDateData extends ParsesDate {

    // TODO into Enum
    // https://developers.google.com/transit/gtfs/reference#calendar_datestxt
    public static final int ADDED = 1;
    public static final int REMOVED = 2;

    @JsonProperty("service_id")
    private String serviceId;
    private TramDate date;

    @JsonProperty("exception_type")
    private int exceptionType;

    public CalendarDateData() {
        // deserialization
    }

    @JsonProperty("date")
    private void setDate(String text) {
        date = parseTramDate(text);
    }

    public IdFor<Service> getServiceId() {
        return Service.createId(serviceId);
    }

    public TramDate getDate() {
        return date;
    }

    public int getExceptionType() {
        return exceptionType;
    }


    @Override
    public String toString() {
        return "CalendarDateData{" +
                "serviceId='" + serviceId + '\'' +
                ", date=" + date +
                ", exceptionType=" + exceptionType +
                "} ";
    }
}
