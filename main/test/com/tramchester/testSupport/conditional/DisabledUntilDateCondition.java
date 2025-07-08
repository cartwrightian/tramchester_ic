package com.tramchester.testSupport.conditional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class DisabledUntilDateCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_NOT_PRESENT = ConditionEvaluationResult.enabled(
            "@DisabledUntilDate is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        LocalDate now = LocalDate.now();
        AnnotatedElement element = context.getElement().orElse(null);
        return findAnnotationFor(element).
                map(matched -> createResult(matched, now)).
                orElse(ENABLED_NOT_PRESENT);
    }

    public static Optional<DisabledUntilDate> findAnnotationFor(final AnnotatedElement element) {
        return findAnnotation(element, DisabledUntilDate.class);
    }

    public static ConditionEvaluationResult createResult(final DisabledUntilDate disabledUntilDate, LocalDate now) {
        final LocalDate enabledFrom = LocalDate.of(disabledUntilDate.year(), disabledUntilDate.month(), disabledUntilDate.day());

        if (now.isBefore(enabledFrom)) {
            return ConditionEvaluationResult.disabled("Disabled since not reached " + enabledFrom);
        } else {
            return ConditionEvaluationResult.enabled("Enabled since on or after " + enabledFrom);
        }
    }
}
