package com.tramchester.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/***
 * Attempts to duplicate the DropWizard startup and validation for configuration
 */
public class StandaloneConfigLoader {
    // use by CLI tools and testing
    public static AppConfiguration LoadConfigFromFile(final Path fullPathToConfig) throws IOException, ConfigurationException {
        final YamlConfigurationFactory<AppConfiguration> factory = getValidatingFactory();

        final FileConfigurationSourceProvider fileProvider = new FileConfigurationSourceProvider();

        final SubstitutingSourceProvider provider = new SubstitutingSourceProvider(fileProvider, App.getEnvVarSubstitutor());

        return factory.build(provider, fullPathToConfig.toString());

    }

    @NotNull
    private static YamlConfigurationFactory<AppConfiguration> getValidatingFactory() {
        final Class<AppConfiguration> klass = AppConfiguration.class;

        final ValidatorFactory validatorFactory = Validators.newValidatorFactory();
        final Validator validator = validatorFactory.getValidator();

        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        final String propertyPrefix = "dw";
        return new YamlConfigurationFactory<>(klass, validator, objectMapper, propertyPrefix);
    }

}
