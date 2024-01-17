package com.tramchester.integration.testSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.testSupport.TestEnv;
import jakarta.ws.rs.core.Cookie;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CookieSupport {

    public static <T extends Location<?>> Cookie createCookieFor(Stream<T> locations, ObjectMapper mapper) throws JsonProcessingException {
        Set<Timestamped> recent = locations.map(location -> new Timestamped(location, TestEnv.LocalNow())).collect(Collectors.toSet());

        RecentJourneys recentJourneys = new RecentJourneys();

        recentJourneys.setTimestamps(recent);

        String recentAsString = RecentJourneys.encodeCookie(mapper,recentJourneys);
        return new Cookie("tramchesterRecent", recentAsString);
    }
}
