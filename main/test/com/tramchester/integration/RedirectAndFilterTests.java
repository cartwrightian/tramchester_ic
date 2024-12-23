package com.tramchester.integration;

import com.tramchester.App;
import com.tramchester.RedirectToAppFilter;
import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class RedirectAndFilterTests {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private URI base;
    private URI app;

    @BeforeEach
    void onceBeforeEachTestRuns() throws URISyntaxException {
        base = new URI("http://localhost:" + appExtension.getLocalPort());
        app = new URI("http://localhost:" + appExtension.getLocalPort() + "/app");
    }

    @Test
    void shouldUnsecureRedirectToAppIfNoHeaderFromELB() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.connect();
        int code = connection.getResponseCode();
        String location = connection.getHeaderField("Location");
        connection.disconnect();

        Assertions.assertEquals(URLStatus.MOVED_TEMPORARILY, code);
        Assertions.assertTrue(location.startsWith("http://"));
        Assertions.assertTrue(location.endsWith("/app"));
    }

    @Test
    void shouldSecureRedirect() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "http");
        connection.connect();
        int code = connection.getResponseCode();
        String location = connection.getHeaderField("Location");
        connection.disconnect();

        Assertions.assertEquals(URLStatus.MOVED_TEMPORARILY, code);
        Assertions.assertTrue(location.startsWith("https://"));
        Assertions.assertTrue(location.endsWith("/"));
    }

    @Test
    void shouldSecureRedirectToAppWithHTTPS() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "https");
        connection.connect();

        String location = connection.getHeaderField("Location");
        int code = connection.getResponseCode();
        connection.disconnect();

        Assertions.assertEquals(URLStatus.MOVED_TEMPORARILY, code);
        Assertions.assertTrue(location.startsWith("https://"));
        Assertions.assertTrue(location.endsWith("/app"));
    }

    @Test
    void shouldNotRedirectTheELBHealthCheck() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.setRequestProperty("User-Agent",RedirectToAppFilter.ELB_HEALTH_CHECKER);
        connection.connect();

        int code = connection.getResponseCode();
        connection.disconnect();

        Assertions.assertEquals(URLStatus.OK, code);
    }

    @Test
    void shouldHaveNoRedirectionIfAppPresentAndSecure() throws IOException {
        HttpURLConnection connection = getConnection(app);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "https");
        connection.connect();

        int code = connection.getResponseCode();
        connection.disconnect();

        // actually getting a 404 here on the integration tests as static bundle wireup not working with the app extension
        // main thing to test here is that not being redirected, and if /app is not being served the acceptance tests
        // will all fail.....
        Assertions.assertNotEquals(URLStatus.MOVED_TEMPORARILY, code);
    }

    @Test
    void shouldRedirectionIfNotSecure() throws IOException {
        HttpURLConnection connection = getConnection(app);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "http");
        connection.connect();

        String location = connection.getHeaderField("Location");
        int code = connection.getResponseCode();
        connection.disconnect();

        Assertions.assertTrue(location.startsWith("https://"));
        Assertions.assertTrue(location.endsWith("/app"));

        Assertions.assertEquals(URLStatus.MOVED_TEMPORARILY, code);
    }

    @NotNull
    private HttpURLConnection getConnection(URI url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        return connection;
    }

}
