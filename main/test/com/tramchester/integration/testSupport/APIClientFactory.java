package com.tramchester.integration.testSupport;

import com.tramchester.acceptance.infra.AcceptanceAppExtenstion;
import jakarta.ws.rs.client.Client;

public class APIClientFactory {

    private final Client client;
    private final String url;

    public APIClientFactory(IntegrationAppExtension appExtension) {
        client = appExtension.client();
        this.url = "http://localhost:" + appExtension.getLocalPort();
    }

    public APIClientFactory(AcceptanceAppExtenstion appExtension) {
        client = appExtension.client();
        this.url = appExtension.getUrl();
    }

    public APIClient clientFor(String endPoint) {
        return new APIClient(client, url, endPoint);
    }
}