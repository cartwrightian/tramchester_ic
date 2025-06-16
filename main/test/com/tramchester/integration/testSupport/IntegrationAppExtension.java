package com.tramchester.integration.testSupport;


import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.AppConfiguration;
import io.dropwizard.core.Application;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

public class IntegrationAppExtension extends DropwizardAppExtension<AppConfiguration> {

    // NOTE See TestConfig for set-up of server, including gzip

    public IntegrationAppExtension(Class<? extends Application<AppConfiguration>> applicationClass, AppConfiguration configuration) {
        super(new DropwizardTestSupport<>(applicationClass, configuration));
    }

    @Override
    public void after() {
        final App app =  getApplication();
        if (app!=null) {
            final GuiceContainerDependencies deps = app.getDependencies();
            if (deps != null) {
                deps.close();
            }
        }
        super.after();
    }
}
