package com.tramchester.integration.cloud;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.BufferedReader;
import java.io.IOException;

public class StubbedAWSServer  {
    public static final String ACCESS_TOKEN_HEADER = "X-aws-ec2-metadata-token-ttl-seconds";

    private String metadata;
    private String calledUrl;
    private String postedData;
    private Server server;
    private final Handler handler;
    private String contentHeader;
    private final String metaDataAccessToken;

    public StubbedAWSServer() {
        this("");
    }

    public StubbedAWSServer(String metaDataAccessToken) {
        this.metaDataAccessToken = metaDataAccessToken;
        handler = new Handler();
    }

    public void run(String metadata) throws Exception {
        this.metadata = metadata;
        server = new Server(8080);
        server.setHandler(handler);
        server.start();
    }

    public void run() throws Exception {
        run("unused");
    }

    public String getCalledUrl() {
        return calledUrl;
    }

    public String getPutData() {
        return postedData;
    }

    public void stopServer() throws Exception {
        server.stop();
        while (server.isRunning()) {
            Thread.sleep(30);
        }
        server.destroy();
    }

    public String getContentHeader() {
        return contentHeader;
    }

    private class Handler extends AbstractHandler{
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            calledUrl = request.getRequestURL().toString();
            contentHeader = request.getHeader("Content-Type");
            if (request.getMethod().equals("GET")) {
                processGet(baseRequest, response);
            } else if (request.getMethod().equals("PUT")) {
                processPut(baseRequest, response);
            }
        }

        private void processPut(Request request, HttpServletResponse response) throws IOException {
            final String metaDataAccessTokenRequest = request.getHeader(ACCESS_TOKEN_HEADER);
            if (metaDataAccessTokenRequest!=null) {
                if (metaDataAccessToken.isEmpty()) {
                    throw new RuntimeException(ACCESS_TOKEN_HEADER + " was set but no token is set");
                }
                response.getWriter().write(metaDataAccessToken);
                postedData = metaDataAccessTokenRequest;
            } else {
                BufferedReader reader = request.getReader();
                StringBuilder incoming = new StringBuilder();
                reader.lines().forEach(incoming::append);
                postedData = incoming.toString();
            }
            response.setStatus(HttpServletResponse.SC_OK);
            request.setHandled(true);
        }

        private void processGet(Request baseRequest, HttpServletResponse response) throws IOException {
            response.setContentType("text/html;charset=utf-8");

            if (!metaDataAccessToken.isEmpty()) {
                final String token = baseRequest.getHeader("X-aws-ec2-metadata-token");
                if (token==null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    baseRequest.setHandled(true);
                    return;
                }
                if (!token.equals(metaDataAccessToken)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    baseRequest.setHandled(true);
                    return;
                }
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(metadata);
            baseRequest.setHandled(true);
        }
    }

}
