package com.tramchester.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.HttpClients;
import org.neo4j.server.http.cypher.format.api.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import static java.lang.String.format;

@LazySingleton
public class FetchInstanceMetadata implements FetchMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FetchInstanceMetadata.class);

    private static final int TIMEOUT = 4000;
    private static final java.lang.String USER_DATA_PATH = "/latest/user-data";
    private final TramchesterConfig config;

    @Inject
    public FetchInstanceMetadata(TramchesterConfig tramchesterConfig) {
        this.config = tramchesterConfig;
    }

    public String getUserData() {
        String urlFromConfig = config.getInstanceDataUrl();
        if (urlFromConfig.isEmpty()) {
            logger.warn("No url for instance meta data, returning empty");
            return "";
        }

        try {
            URL baseUrl = new URL(urlFromConfig);
            URL url = new URL(baseUrl, USER_DATA_PATH);
            return getDataFrom(url);
        } catch (MalformedURLException e) {
            logger.warn(format("Unable to fetch instance metadata from %s and %s", urlFromConfig, USER_DATA_PATH),e);
            return "";
        }
    }

    private String getDataFrom(URL url) {
        String host = url.getHost();
        int port = (url.getPort()==-1) ? 80 : url.getPort();
        logger.info(format("Attempt to getPlatformById instance user data from host:%s port%s url:%s",
                host, port, url));
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url.toString());
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT).build();
        httpGet.setConfig(config);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            HttpResponse result = httpClient.execute(httpGet);
            HttpEntity entity = result.getEntity();
            entity.writeTo(stream);
            return stream.toString();
        }
        catch (ConnectTimeoutException timeout) {
            logger.info("Timed out getting meta data for '"+url+"', not running in cloud");
        }
        catch (SocketException socketException) {
            String connectFailedMsg = format("Connect to %s:%s [/%s] failed: Connection refused", host, port, host);
            String exceptionMessage = socketException.getMessage();
            String msg = createDiagnosticMessage("SocketException", url, exceptionMessage);
            if ("Host is down".equals(exceptionMessage) || (connectFailedMsg.equals(exceptionMessage))) {
                logger.info(msg);
            }
            else {
                logger.warn(msg, socketException);
            }
        }
        catch (ConnectionException connectionException) {
            String exceptionMessage = connectionException.getMessage();
            logger.warn(createDiagnosticMessage("ConnectionException", url, exceptionMessage),
                    connectionException);
        }
        catch (IOException ioException) {
            String exceptionMessage = ioException.getMessage();
            logger.warn(createDiagnosticMessage("IOException", url, exceptionMessage), ioException);
        }
        return "";

    }

    private String createDiagnosticMessage(String exceptionName, URL url, String exceptionMessage) {
        return format("type:%s message:'%s' Diagnostic: Cannot connect to '%s' likely not running in the cloud",
                exceptionName, exceptionMessage, url);
    }
}
