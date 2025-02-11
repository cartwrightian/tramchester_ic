package com.tramchester.integration.testSupport;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;
import org.junit.jupiter.api.extension.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/***
 * Executes tests if and only if currently configured modes match exactly those given
 * i.e. For config [tram,train] tram or train only will NOT match, only (Tram, Train) will match
 ***/
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(OnlyIfModesExact.ExactMatchToConfiguredModesExtension.class)
public @interface OnlyIfModesExact {
    TransportMode[] value();

    class ExactMatchToConfiguredModesExtension implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
            final Optional<Method> maybeMethod = context.getTestMethod();
            if (maybeMethod.isPresent()) {
                final Method method = context.getRequiredTestMethod();
                final var annotation = method.getDeclaredAnnotation(OnlyIfModesExact.class);
                if (annotation == null) {
                    return ConditionEvaluationResult.enabled("no OnlyIfModesExact annotation");
                }

                final TramchesterConfig config = (TramchesterConfig) context.getStore(ExtensionContext.Namespace.GLOBAL).get("tramchester.config");
                if (config==null) {
                    throw new ExtensionConfigurationException("ConfigParameterResolver not conigured? config not found");
                }
                final EnumSet<TransportMode> configuredModes = config.getTransportModes();
                final List<TransportMode> fromAnnotation = Arrays.asList(annotation.value());

                final boolean match = configuredModes.equals(EnumSet.copyOf(fromAnnotation));

                if (match) {
                    return ConditionEvaluationResult.enabled("modes matched config");
                } else {
                    return ConditionEvaluationResult.disabled("modes did not match, config was " + configuredModes);
                }

            }
            return ConditionEvaluationResult.enabled("");
        }
    }
}