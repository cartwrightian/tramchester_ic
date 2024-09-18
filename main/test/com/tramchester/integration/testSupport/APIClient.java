package com.tramchester.integration.testSupport;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;


public class APIClient {
    private static final Logger logger = LoggerFactory.getLogger(APIClient.class);
    private static final int TIMEOUT_MS = 20 * 1000;

    private final Invocation.Builder builder;

    private final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final String RFC1123_DATE_FORMAT_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private final String url;

    APIClient(Client client, String baseURL, String endPoint) {
        this.url = baseURL + "/api/" + endPoint;
        WebTarget target = client.target(url);
        target.property(ClientProperties.READ_TIMEOUT, TIMEOUT_MS);

        builder = target.request(MediaType.APPLICATION_JSON);
    }

    public static Response getApiResponse(APIClientFactory factory, String endPoint) {
        return factory.clientFor(endPoint).getApiResponse();
    }

    public static Response getApiResponse(APIClientFactory factory, String endPoint, Date lastMod) {
        return factory.clientFor(endPoint).getApiResponse(lastMod);
    }

    public static Response getApiResponse(APIClientFactory factory, String endPoint, List<Cookie> cookies) {
        return factory.clientFor(endPoint).getApiResponse(cookies);
    }

    public static <T> Response postAPIRequest(APIClientFactory factory, String endPoint, T payload, List<Cookie> cookies) {
        return factory.clientFor(endPoint).postAPIRequest(payload, cookies);
    }

    public static <T> Response postAPIRequest(APIClientFactory factory, String endpoint, T payload) {
        return factory.clientFor(endpoint).postAPIRequest(payload);
    }

    private void setCookie(Cookie cookie) {
        builder.cookie(cookie);
    }

    private void setLastMod(final Date currentLastMod) {
        // the locale here matters!
        final SimpleDateFormat format = new SimpleDateFormat(RFC1123_DATE_FORMAT_PATTERN, Locale.US);
        format.setTimeZone(GMT_TIME_ZONE);
        builder.header("If-Modified-Since", format.format(currentLastMod));
    }

    private Response get() {
        return builder.get();
    }

    private Response post(Entity<?> entity) {
        return builder.post(entity);
    }

    public Response getApiResponse() {
        return getApiResponse(Collections.emptyList());
    }

    public Response getApiResponse(List<Cookie> cookieList) {
        logger.info(format("GET from %s with cookies %s", url, cookieList));
        cookieList.forEach(this::setCookie);
        return get();
    }

    public <T> Response postAPIRequest(T payload, List<Cookie> cookies) {
        logger.info(format("POST to %s with %s cookies %s", url, payload, cookies));
        cookies.forEach(this::setCookie);
        Entity<T> entity = Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE);
        return post(entity);
    }

    public <T> Response postAPIRequest(T payload) {
        logger.info(format("POST to %s with %s no cookies", url, payload));
        Entity<T> entity = Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE);
        return post(entity);
    }

    public Response getApiResponse(Date lastMod) {
        logger.info(format("GET from %s with last modified %s", url, lastMod));
        setLastMod(lastMod);
        return get();
    }

}
