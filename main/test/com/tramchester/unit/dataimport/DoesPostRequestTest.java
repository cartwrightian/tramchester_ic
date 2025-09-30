package com.tramchester.unit.dataimport;

import com.tramchester.dataimport.DoesPostRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoesPostRequestTest {

    public static final String PLEASE_FAIL = "pleaseFail";
    StubbedServer stubbedServer;

    @BeforeEach
    void onceBeforeEachTest() throws Exception {
        stubbedServer = new StubbedServer();
        stubbedServer.start();
    }

    @AfterEach
    void onceAfterEachTest() throws Exception {
        stubbedServer.stop();
    }

    @Test
    void shouldPostSomethingAndProcessResponse() {
        DoesPostRequest doesPostRequest = new DoesPostRequest();

        String url = "http://localhost:8080/path";
        String response = doesPostRequest.post(URI.create(url), "someText");

        assertEquals("application/x-www-form-urlencoded", stubbedServer.getContentType());
        assertEquals(url, stubbedServer.getCalledURL());
        assertEquals("someText", stubbedServer.getPostedData());

        assertEquals("someResponseText", response);
    }

    @Test
    void shouldPostAndHandleFailure() {
        DoesPostRequest doesPostRequest = new DoesPostRequest();

        String url = "http://localhost:8080/path";
        String response = doesPostRequest.post(URI.create(url), PLEASE_FAIL);

        assertEquals("application/x-www-form-urlencoded", stubbedServer.getContentType());
        assertEquals(url, stubbedServer.getCalledURL());
        assertEquals(PLEASE_FAIL, stubbedServer.getPostedData());

        assertTrue(response.isEmpty());
    }

    private static class StubbedServer {
        private final Server server;
        private final StubbedHandler handler;

        public StubbedServer() {
            handler = new StubbedHandler();
            server = new Server(8080);
            server.setHandler(handler);
        }

        public void start() throws Exception {
            server.start();
        }

        public void stop() throws Exception {
            server.stop();
            while (server.isRunning()) {
                Thread.sleep(30);
            }
            server.destroy();
        }

        public String getContentType() {
            return handler.contentHeader;
        }

        public String getCalledURL() {
            return handler.calledUrl;
        }

        public String getPostedData() {
            return handler.postedData;
        }
    }

    private static class StubbedHandler extends Handler.Abstract {

        private String calledUrl;
        private String contentHeader;
        private String postedData;

        @Override
        public boolean handle(Request baseRequest, Response response, Callback callback) throws Exception {
            calledUrl = baseRequest.getHttpURI().toString(); //.getRequestURL().toString();
            contentHeader = baseRequest.getHeaders().get("Content-Type");
            final boolean result;
            if (baseRequest.getMethod().equals("POST")) {
                result = processPut(baseRequest, response);
                callback.succeeded();
            } else {
                result = false;
            }
            return result;
        }

        private boolean processPut(Request baseRequest, Response response) throws IOException {
            postedData = Content.Source.asString(baseRequest, UTF_8);

            if (PLEASE_FAIL.equals(postedData)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                try (OutputStream sink = Content.Sink.asOutputStream(response)) {
                    sink.write("someResponseText".getBytes());
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            }
            return true;
        }


    }
}
