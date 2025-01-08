package com.tramchester.unit.dataimport;

import com.tramchester.dataimport.DoesPostRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;

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

    private static class StubbedHandler extends AbstractHandler {

        private String calledUrl;
        private String contentHeader;
        private String postedData;

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            calledUrl = request.getRequestURL().toString();
            contentHeader = request.getHeader("Content-Type");
            if (request.getMethod().equals("PUT")) {
                processPut(baseRequest, response);
            }
        }

        private void processPut(Request baseRequest, HttpServletResponse response) throws IOException {
            BufferedReader reader = baseRequest.getReader();
            StringBuilder incoming = new StringBuilder();
            reader.lines().forEach(incoming::append);
            postedData = incoming.toString();
            if (PLEASE_FAIL.equals(postedData)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.getWriter().write("someResponseText");
                response.setStatus(HttpServletResponse.SC_OK);
            }
            baseRequest.setHandled(true);
        }
    }
}
