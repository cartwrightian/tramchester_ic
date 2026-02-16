package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.reflections.ReflectionUtils.Methods;

public class ConfigReference<T> {

    private final String config;
    private T resolved;

    public ConfigReference(@JsonProperty("config") String config) {
        this.config = config;
    }

    public ConfigReference(T defaultValue) {
        this.resolved = defaultValue;
        this.config = "";
    }

    public T resolve(final TramchesterConfig configuration) {
        if (resolved != null) {
            return resolved;
        }

        final Reflections reflections = new Reflections(AppConfiguration.class);

        final Set<Method> all = reflections.get(Methods.of(AppConfiguration.class));

        final Stream<Method> annotatedMethods = all.stream().filter(method -> method.isAnnotationPresent(JsonProperty.class));

        final Optional<Method> found = annotatedMethods.
                filter(method -> method.getDeclaredAnnotation(JsonProperty.class).value().equals(config)).
                findFirst();

        if (found.isPresent()) {
            final Method method = found.get();
            try {
                Object result = method.invoke(configuration);
                resolved = (T) result;
                return resolved;
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
                throw new RuntimeException("Could not resolve config name " + config, exception);
            }
        }

        throw new RuntimeException("Could not find config matching name " + config);

    }

}
