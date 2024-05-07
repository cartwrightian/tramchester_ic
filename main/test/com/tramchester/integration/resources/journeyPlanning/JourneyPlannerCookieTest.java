package com.tramchester.integration.resources.journeyPlanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerCookieTest {
    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneyPlannerResource.class));

    private final ObjectMapper mapper = new ObjectMapper();
    private LocalDateTime now;
    private JourneyResourceTestFacade journeyPlanner;
    private StationRepository stationRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        GuiceContainerDependencies dependencies = app.getDependencies();

        journeyPlanner = new JourneyResourceTestFacade(appExtension);
        stationRepository = dependencies.get(StationRepository.class);
        now = TestEnv.LocalNow();
    }

    @Test
    void shouldSetCookieForRecentJourney() throws IOException {
        Station start = TramStations.Altrincham.from(stationRepository);
        Station end = TramStations.StPetersSquare.from(stationRepository);

        TramTime time = TramTime.ofHourMins(now.toLocalTime());

        Response result = getResponseForJourney(start, end, time, now.toLocalDate(), Collections.emptyList());

        Assertions.assertEquals(200, result.getStatus());

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        Assertions.assertEquals(2,recentJourneys.getTimeStamps().size());
        assertTrue(recentJourneys.getTimeStamps().contains(new Timestamped(start, now)));
        assertTrue(recentJourneys.getTimeStamps().contains(new Timestamped(end, now)));
    }

    @Test
    void shouldUpdateCookieForRecentJourney() throws IOException {
        Station start = TramStations.Altrincham.from(stationRepository);
        Station end = TramStations.StPetersSquare.from(stationRepository);

        TramTime time = TramTime.ofHourMins(now.toLocalTime());

        // cookie with ashton
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped ashtonCookieEntry = new Timestamped(TramStations.Ashton.getId(), now, LocationType.Station);
        recentJourneys.setTimeStamps(Sets.newHashSet(ashtonCookieEntry));
        Cookie cookie = new Cookie("tramchesterRecent", RecentJourneys.encodeCookie(mapper,recentJourneys));

        // journey to bury
        Response response = getResponseForJourney(start, end, time, now.toLocalDate(), List.of(cookie));

        Assertions.assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);

        // ashton, bury and man airport now in cookie
        Set<Timestamped> recents = result.getTimeStamps();
        Assertions.assertEquals(3, recents.size());
        assertTrue(recents.contains(new Timestamped(start, now)));
        assertTrue(recents.contains(ashtonCookieEntry));
        assertTrue(recents.contains(new Timestamped(end, now)));
    }

    @Test
    void shouldPreseverCookieForRecentJourneyIfDifferentLocationType() throws IOException {
        Station start = TramStations.Altrincham.from(stationRepository);
        Station end = TramStations.StPetersSquare.from(stationRepository);

        TramTime time = TramTime.ofHourMins(now.toLocalTime());

        // cookie with ashton as
        RecentJourneys recentJourneys = new RecentJourneys();
        IdFor<StationGroup> idForStockportLocality = StringIdFor.convert(KnownLocality.Stockport.getId(), StationGroup.class);
        Timestamped stockportLocalityCookie = new Timestamped(idForStockportLocality, now, LocationType.StationGroup);
        recentJourneys.setTimeStamps(Sets.newHashSet(stockportLocalityCookie));
        Cookie cookie = new Cookie("tramchesterRecent", RecentJourneys.encodeCookie(mapper,recentJourneys));

        // journey to bury
        Response response = getResponseForJourney(start, end, time, now.toLocalDate(), List.of(cookie));

        Assertions.assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);

        // ashton, bury and man airport now in cookie
        Set<Timestamped> recents = result.getTimeStamps();
        Assertions.assertEquals(3, recents.size());
        assertTrue(recents.contains(new Timestamped(start, now)));
        assertTrue(recents.contains(stockportLocalityCookie));
        assertTrue(recents.contains(new Timestamped(end, now)));
    }

    @Test
    void shouldOnlyCookiesForDestinationIfLocationSent() throws IOException {
        LatLong latlong = new LatLong(53.3949553,-2.3580997999999997 );
        MyLocation start = new MyLocation(latlong);
        Station end = TramStations.ManAirport.from(stationRepository);

        TramTime time = TramTime.ofHourMins(now.toLocalTime());

        Response response = getResponseForJourney(start, end, time,  now.toLocalDate(), Collections.emptyList());
        Assertions.assertEquals(200, response.getStatus());

        RecentJourneys result = getRecentJourneysFromCookie(response);
        Set<Timestamped> recents = result.getTimeStamps();
        Assertions.assertEquals(1, recents.size());
        // checks ID only
        assertTrue(recents.contains(new Timestamped(end, now)));
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        assertNotNull(recent);
        Assertions.assertEquals("/api", recent.getPath());
        Assertions.assertEquals("localhost", recent.getDomain());
        String value = recent.toCookie().getValue();
        return RecentJourneys.decodeCookie(mapper, value);
    }

    private Response getResponseForJourney(Location<?> start, Location<?> end, TramTime time, LocalDate date, List<Cookie> cookies) {

        JourneyQueryDTO query = JourneyQueryDTO.create(date, time, start, end, false, 2, false);

        query.setMaxNumResults(2);

        return journeyPlanner.getResponse(false, cookies, query);
    }
}
