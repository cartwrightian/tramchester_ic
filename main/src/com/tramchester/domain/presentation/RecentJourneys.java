package com.tramchester.domain.presentation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;


// encoded into cooke for front-end

public class RecentJourneys  {

    private Set<Timestamped> timestamps;

    public RecentJourneys() {
        // deserialisation
    }

    @JsonIgnore
    public RecentJourneys setTimestamps(Set<Timestamped> timestamps) {
        setTimeStamps(timestamps);
        return this;
    }

    public static RecentJourneys empty() {
        return new RecentJourneys().setTimestamps(new HashSet<>());
    }

    public static RecentJourneys decodeCookie(ObjectMapper objectMapper, String cookieString) throws IOException {
        String decoded = URLDecoder.decode(cookieString, StandardCharsets.UTF_8);
        return objectMapper.readValue(decoded, RecentJourneys.class);
    }

    public static String encodeCookie(ObjectMapper objectMapper, RecentJourneys recentJourneys) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(recentJourneys);
        return URLEncoder.encode(json, StandardCharsets.UTF_8);
    }

    public Stream<Timestamped> stream() {
        return timestamps.stream();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecentJourneys that = (RecentJourneys) o;
        return timestamps.equals(that.timestamps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamps);
    }

    // recentIds was old name
    @JsonAlias({"recentIds", "records"})
    public void setTimeStamps(Set<Timestamped> timestamps) {
        this.timestamps = timestamps;
    }

    @JsonProperty(value = "records")
    public Set<Timestamped> getTimeStamps() {
        return timestamps;
    }

    @Override
    public String toString() {
        return "RecentJourneys{" +
                "timestamps=" + timestamps +
                '}';
    }
}
