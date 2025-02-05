package com.tramchester.testSupport.conditional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.net.Socket;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class RequiresNetworkCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_NOT_PRESENT = ConditionEvaluationResult.enabled(
            "@DisabledUntilDate is not present");

    static boolean networkAvailable = checkForActiveInterface();

    private static boolean checkForActiveInterface() {
        boolean opened=false;
        try {
            final Socket socket = new Socket("www.google.com", 443);
            opened = true;
            socket.close();
        } catch (IOException e) {
            // can't open socket
        }
        return opened;
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final AnnotatedElement element = context.getElement().orElse(null);
        return findAnnotation(element, RequiresNetwork.class).map(this::createResult).
                orElse(ENABLED_NOT_PRESENT);
    }

    private ConditionEvaluationResult createResult(RequiresNetwork requiresNetwork) {

        if (networkAvailable) {
            return ConditionEvaluationResult.enabled("network present");
        } else {
            return ConditionEvaluationResult.disabled("network missing");
        }
    }
}
