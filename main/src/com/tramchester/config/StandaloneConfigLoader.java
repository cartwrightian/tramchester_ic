package com.tramchester.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.core.Configuration;
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

    public static AppConfiguration LoadConfigFromFile(final Path fullPathToConfig) throws ConfigurationException, IOException {
        return LoadConfigFromFile(fullPathToConfig, AppConfiguration.class);
    }

    // use by CLI tools and testing
    public static <T extends Configuration> T LoadConfigFromFile(final Path fullPathToConfig, Class<T> klass) throws IOException, ConfigurationException {
        Path configDir = fullPathToConfig.getParent();
        final YamlConfigurationFactory<T> factory = getValidatingFactory(klass);

        final FileConfigurationSourceProvider fileProvider = new FileConfigurationSourceProvider();

        ConfigurationSourceProvider substitutingProvider = App.getConfigSourceProvider(fileProvider, configDir);

        return factory.build(substitutingProvider, fullPathToConfig.toString());
    }

    @NotNull
    private static <T extends Configuration> YamlConfigurationFactory<T> getValidatingFactory(Class<T> klass) {

        final ValidatorFactory validatorFactory = Validators.newValidatorFactory();
        final Validator validator = validatorFactory.getValidator();

        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        final String propertyPrefix = "dw";
        return new YamlConfigurationFactory<>(klass, validator, objectMapper, propertyPrefix);
    }

}
