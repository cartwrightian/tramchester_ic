package com.tramchester.testSupport;

import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.testSupport.testTags.TramApp;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class TramAppTestExtension implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        try {
            beforeAll(context.getRequiredTestClass());
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    private void beforeAll(final Class<?> testClass) {
        final Field found = getAppExtensionField(testClass);

        try {
            IntegrationAppExtension contents = (IntegrationAppExtension) found.get(testClass);
            contents.before();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try {
            afterAll(context.getRequiredTestClass());
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    private void afterAll(final Class<?> testClass) {
        Field found = getAppExtensionField(testClass);
        final IntegrationAppExtension contents;
        try {
            contents = (IntegrationAppExtension) found.get(testClass);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        if (contents!=null) {
            throw new IllegalStateException("TramApp field/extension not set to null, will memory leak");
        }
    }

    private static Field getAppExtensionField(Class<?> testClass) {
        final List<Field> search = Arrays.stream(testClass.getDeclaredFields()).
                filter(field -> Modifier.isStatic(field.getModifiers())).
                filter(field -> field.isAnnotationPresent(TramApp.class)).
                filter(field -> field.getType().isAssignableFrom(IntegrationAppExtension.class)).toList();
        if (search.isEmpty()) {
            throw new IllegalStateException("Unable to find TestAppExtension field");
        }
        if (search.size()!=1) {
            throw new IllegalStateException("Found wrong number of TestAppExtension fields " + search);
        }
        Field found = search.getFirst();
        found.trySetAccessible();
        return found;
    }

}
