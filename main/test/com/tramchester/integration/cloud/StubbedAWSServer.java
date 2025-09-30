package com.tramchester.integration.cloud;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.OutputStream;

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

    private class Handler extends org.eclipse.jetty.server.Handler.Abstract {


        @Override
        public boolean handle(Request baseRequest, Response response, Callback callback) throws Exception {
            calledUrl = baseRequest.getHttpURI().toString();
            contentHeader = baseRequest.getHeaders().get("Content-Type");
            final String method = baseRequest.getMethod();
            final boolean result;
            if (method.equals("GET")) {
                result = processGet(baseRequest, response);
            } else if (method.equals("PUT")) {
                result = processPut(baseRequest, response);
            } else {
                result = false;
            }
            callback.succeeded();
            return result;
        }

        private boolean processPut(Request request, Response response) throws IOException {
            final String metaDataAccessTokenRequest = request.getHeaders().get(ACCESS_TOKEN_HEADER);
            if (metaDataAccessTokenRequest!=null) {
                if (metaDataAccessToken.isEmpty()) {
                    throw new RuntimeException(ACCESS_TOKEN_HEADER + " was set but no token is set");
                }
                try (OutputStream sink = Content.Sink.asOutputStream(response)) {
                    sink.write(metaDataAccessToken.getBytes());
                }
                postedData = metaDataAccessTokenRequest;
            } else {
                postedData = Content.Source.asString(request);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        private boolean processGet(Request baseRequest, Response response) throws IOException {
            HttpFields.Mutable headers = response.getHeaders();
            headers.add(HttpHeader.CONTENT_TYPE, "text/html;charset=utf-8");

            if (!metaDataAccessToken.isEmpty()) {
                final String token = baseRequest.getHeaders().get("X-aws-ec2-metadata-token");
                if (token==null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return true;
                }
                if (!token.equals(metaDataAccessToken)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return true;
                }
            }
            response.setStatus(HttpServletResponse.SC_OK);
            try (OutputStream sink = Content.Sink.asOutputStream(response)) {
                sink.write(metadata.getBytes());
            }
            return true;
        }

    }

}
