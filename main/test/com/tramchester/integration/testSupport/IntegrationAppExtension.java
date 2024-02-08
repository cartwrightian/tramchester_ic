package com.tramchester.integration.testSupport;


import com.tramchester.config.AppConfiguration;
import io.dropwizard.core.Application;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

public class IntegrationAppExtension extends DropwizardAppExtension<AppConfiguration> {

    // NOTE See TestConfig for set-up of server, including gzip

    public IntegrationAppExtension(Class<? extends Application<AppConfiguration>> applicationClass, AppConfiguration configuration) {
        super(applicationClass, configuration);

    }

}
