package com.tramchester.integration.testSupport.config;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.testTags.DualTest;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.TrainTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

public class ConfigParameterResolver implements ParameterResolver {

    public static final String PARAMETER_KEY = "com.tramchester.config";
    private static final String dualTest = getTagName(DualTest.class);

    private static final String trainTest = getTagName(TrainTest.class);
    private static final String gmTest = getTagName(GMTest.class);

    private static final TramchesterConfig tramOnly = new IntegrationTramTestConfig();
    private static final TramchesterConfig tramAndTrain =  new RailAndTramGreaterManchesterConfig();
    private static final TramchesterConfig trainOnly = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.National);

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(TramchesterConfig.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        final Set<String> testTags = extensionContext.getTags();

        final Optional<String> haveOverride = extensionContext.getConfigurationParameter(PARAMETER_KEY);

        final TramchesterConfig config;
        if (testTags.contains(dualTest)) {
            config = haveOverride.map(override -> getExpectedOverride(override, tramAndTrain)).orElse(tramOnly);
        }  else if (testTags.contains(gmTest)) {
            config = haveOverride.map(override -> getExpectedOverride(override, tramAndTrain)).orElse(trainOnly);
        } else if (testTags.contains(trainTest)) {
            config = haveOverride.map(override -> getExpectedOverride(override, trainOnly)).orElse(tramAndTrain);
        }
        else {
            throw new ExtensionConfigurationException("ConfigParameterResolver and overrides not defined for tags " + testTags);
        }

        extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put("tramchester.config", config);
        return config;
    }

    @NotNull
    private static TramchesterConfig getExpectedOverride(final String overrideName, final TramchesterConfig expected) {
        final String expectedClassName = (expected.getClass().getSimpleName());
        if (overrideName.equals(expectedClassName)) {
            return expected;
        } else {
            throw new ExtensionConfigurationException(format("Unknown test config provided for '%s',' name '%s' expected '%s'",
                    PARAMETER_KEY, overrideName, expectedClassName));
        }
    }

    private static <T> String getTagName(Class<T> theClass) {
        List<Annotation> annotations = Arrays.asList(theClass.getAnnotations());
        List<Annotation> tags = annotations.stream().filter(annotation -> annotation.annotationType().equals(Tag.class)).toList();
        if (tags.size()!=1) {
            throw new ExtensionConfigurationException(format("Unexpected number (%s) of Tag attributes for %s", tags.size(), theClass.getSimpleName()));
        }
        Annotation tagAnnotation = tags.get(0);
        Tag tag = (Tag) tagAnnotation;

        return tag.value();

    }
}

