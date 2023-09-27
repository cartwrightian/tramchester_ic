package com.tramchester;

import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.IOException;
import java.io.InputStream;

/***
 * If base provider throws then use the provided fallback provider to try again
 */
public class FallbackConfigurationSourceProvider implements ConfigurationSourceProvider {
    private final ConfigurationSourceProvider provider;
    private final ConfigurationSourceProvider fallbackProvider;

    public FallbackConfigurationSourceProvider(ConfigurationSourceProvider provider, ConfigurationSourceProvider fallbackProvider) {
        this.provider = provider;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    public InputStream open(String path) throws IOException {
        try {
            return provider.open(path);
        }
        catch (IOException initialFailed) {
            return fallbackProvider.open(path);
        }
    }
}
