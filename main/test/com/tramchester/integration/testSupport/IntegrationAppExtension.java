package com.tramchester.integration.testSupport;


import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationAppExtension extends DropwizardAppExtension<AppConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationAppExtension.class);
    private APIClientFactory factory;

    // NOTE See TestConfig for set-up of server, including gzip

    public IntegrationAppExtension(final AppConfiguration configuration) {
        this(new DropwizardTestSupport<>(App.class, configuration));
    }

    public IntegrationAppExtension(final DropwizardTestSupport<AppConfiguration> testSupport) {
        super(testSupport);
    }

    @Override
    public void before() throws Exception {
        super.before();
        factory = null;
    }

    @Override
    public void after() {
        super.after();

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
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        super.afterAll(extensionContext);
    }

    public APIClientFactory getApiClientFactory() {
        if (factory==null) {
            factory = new APIClientFactory(this);
        }
        return factory;
    }
}
