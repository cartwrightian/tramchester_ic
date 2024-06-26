package com.tramchester.resources;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.ProvidesNow;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import static java.lang.String.format;

public class UsesRecentCookie extends TransportResource {
    private static final Logger logger = LoggerFactory.getLogger(UsesRecentCookie.class);

    public static final String TRAMCHESTER_RECENT = "tramchesterRecent";

    private static final int VERSION = 1;
    private static final int ONE_HUNDRED_DAYS_AS_SECS = 60 * 60 * 24 * 100;

    private final UpdateRecentJourneys updateRecentJourneys;
    private final ObjectMapper mapper;

    public UsesRecentCookie(UpdateRecentJourneys updateRecentJourneys, ProvidesNow providesNow) {
        super(providesNow);
        logger.info("created");
        this.updateRecentJourneys = updateRecentJourneys;
        this.mapper = JsonMapper.builder().addModule(new AfterburnerModule()).build();
    }

    protected RecentJourneys recentFromCookie(Cookie cookie) {
        if (cookie==null) {
            return RecentJourneys.empty();
        }
        String cookieString = cookie.getValue();
        try {
            RecentJourneys recent = RecentJourneys.decodeCookie(mapper,cookieString);
            return recent==null ? RecentJourneys.empty() : recent;
        } catch (IOException e) {
            logger.warn("Unable to decode cookie for recent journeys: "+cookieString, e);
            return RecentJourneys.empty();
        }
    }

    protected NewCookie createRecentCookie(final Cookie current, Location<?> start, Location<?> dest,
                                           boolean secure, URI baseURI) throws JsonProcessingException {
        logger.info(format("Updating recent stations cookie with %s and %s ", start.getId(), dest.getId()));

        RecentJourneys recentJourneys = recentFromCookie(current);

        if (shouldAddToCookie(start)) {
            recentJourneys = updateRecentJourneys.createNewJourneys(recentJourneys, providesNow, start);
        }
        if (shouldAddToCookie(dest)) {
            recentJourneys = updateRecentJourneys.createNewJourneys(recentJourneys, providesNow, dest);
        }

        // NOTE: SameSite is set via ResponseCookieFilter as NewCookie can't set SameSite (yet, TODO)
        String encoded = RecentJourneys.encodeCookie(mapper, recentJourneys);
        return new NewCookie(TRAMCHESTER_RECENT, encoded, "/api", baseURI.getHost(), VERSION,
                "tramchester recent journeys", ONE_HUNDRED_DAYS_AS_SECS, secure);
    }

    private static boolean shouldAddToCookie(Location<?> start) {
        return start.getLocationType() == LocationType.Station || start.getLocationType() == LocationType.StationGroup;
    }

}
