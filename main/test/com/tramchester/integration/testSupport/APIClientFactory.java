package com.tramchester.integration.testSupport;

import com.tramchester.acceptance.infra.AcceptanceAppExtenstion;
import com.tramchester.config.AppConfiguration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.Client;

public class APIClientFactory {

    private final Client client;
    private final String url;

    APIClientFactory(final DropwizardAppExtension<AppConfiguration> appExtension) {
        client = appExtension.client();
        this.url = "http://localhost:" + appExtension.getLocalPort();
    }

    // TODO Private
    public APIClientFactory(final AcceptanceAppExtenstion appExtension) {
        client = appExtension.client();
        this.url = appExtension.getUrl();
    }

    public APIClient clientFor(final String endPoint) {
        return new APIClient(client, url, endPoint);
    }

    public void close() {
        client.close();
    }
}
