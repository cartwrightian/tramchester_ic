package com.tramchester.testSupport;

import com.tramchester.testSupport.testTags.MultiDB;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Optional;
import java.util.Set;

import static com.tramchester.integration.testSupport.config.ConfigParameterResolver.getTagName;

public class GraphTypeConfigResolver implements ParameterResolver {
    private static final String multiDB = getTagName(MultiDB.class);

    public static final String PARAMETER_KEY = "com.tramchester.dbType";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(GraphDBType.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        final Set<String> testTags = extensionContext.getTags();

        final Optional<String> haveOverride = extensionContext.getConfigurationParameter(PARAMETER_KEY);

        final GraphDBType graphDBType;
        if (haveOverride.isPresent()) {
            String text = haveOverride.get();
            graphDBType = GraphDBType.valueOf(text);
        } else {
            graphDBType = TestEnv.getDefaultDBTYpe();
        }

        if (testTags.contains(multiDB)) {
            return graphDBType;
        } else {
            return TestEnv.getDefaultDBTYpe();
        }
    }
}
