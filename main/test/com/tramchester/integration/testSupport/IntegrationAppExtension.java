package com.tramchester.integration.testSupport;


import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import io.dropwizard.core.Application;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationAppExtension extends DropwizardAppExtension<AppConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationAppExtension.class);
    private APIClientFactory factory;

    // NOTE See TestConfig for set-up of server, including gzip

    public IntegrationAppExtension(Class<? extends Application<AppConfiguration>> applicationClass, AppConfiguration configuration) {
        super(new DropwizardTestSupport<>(applicationClass, configuration));
    }

    @Override
    public void before() throws Exception {
        super.before();
        factory = null;
    }

    @Override
    public void after() {
        try {
            if (factory!=null) {
                factory.close();
                factory = null;
            }
            final App app =  getApplication();
            if (app!=null) {
                app.closeDependencies();
            }
        } catch (Exception e) {
            logger.error("exception during after()",e);
        }
        super.after();
    }

    public APIClientFactory getApiClientFactory() {
        if (factory==null) {
            factory = new APIClientFactory(this);
        }
        return factory;
    }
}
