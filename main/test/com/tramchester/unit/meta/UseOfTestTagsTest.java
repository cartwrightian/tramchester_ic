package com.tramchester.unit.meta;

import com.tramchester.App;
import com.tramchester.integration.repository.StationAvailabilityRepositoryTest;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.testSupport.testTags.DualTest;
import com.tramchester.testSupport.testTags.GMTest;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UseOfTestTagsTest {

    private static Reflections reflections;

    @BeforeAll
    public static void onceBeforeAnyTestsRun() {
        reflections = new Reflections(App.class.getPackageName());
    }

    @Test
    void shouldNotHaveGMTestAndDualTestOnOneTest() {
        Set<String> withGMTest = getClassNamesWithAnnotation(GMTest.class);
        Set<String> withDualTest = getClassNamesWithAnnotation(DualTest.class);

        SetUtils.SetView<String> intersection = SetUtils.intersection(withGMTest, withDualTest);

        assertTrue(intersection.isEmpty(), intersection.toString());
    }

    @Test
    void shouldCheckCanMatchClassWithConfigResolver() {
        assertTrue(extendedWithConfigResolver(StationAvailabilityRepositoryTest.class));
    }

    @Test
    void shouldHaveConfigResolverPresentForAllDualTests() {
        Set<Class<?>> withDualTest = reflections.getTypesAnnotatedWith(DualTest.class);

        Set<String> missingResolver = withDualTest.stream().
                filter(klass -> !extendedWithConfigResolver(klass)).
                map(Class::getCanonicalName).
                collect(Collectors.toSet());

        assertTrue(missingResolver.isEmpty());
    }

    @Test
    void shouldHaveExpectedUseOfExtendConfig() {

        // when using config resolver must be tagged as GM or Dual

        Set<Class<?>> extendedWithClasses = reflections.getTypesAnnotatedWith(ExtendWith.class);

        Set<Class<?>> haveConfigResolver = extendedWithClasses.stream().
                filter(this::extendedWithConfigResolver).
                collect(Collectors.toSet());

        assertFalse(haveConfigResolver.isEmpty());

        List<Class<? extends Annotation>> expected = List.of(GMTest.class, DualTest.class);

        Set<Class<?>> missingTestTag = haveConfigResolver.stream().
                filter(klass -> !hasAnyOf(klass, expected)).
                collect(Collectors.toSet());

        assertTrue(missingTestTag.isEmpty(), missingTestTag.toString());
    }

    private boolean hasAnyOf(Class<?> klass, List<Class<? extends Annotation>> expected) {
        for(Class<? extends Annotation> annotation : expected) {
            if (klass.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull Set<String> getClassNamesWithAnnotation(Class<? extends Annotation> annotation) {
        return reflections.getTypesAnnotatedWith(annotation).stream().map(Class::getCanonicalName).collect(Collectors.toSet());
    }

    private boolean extendedWithConfigResolver(final Class<?> klass) {
        final ExtendWith extendsWith = klass.getAnnotation(ExtendWith.class);

        if (extendsWith==null) {
            return false; // some other kind of ExtendsWith present
        }

        long matched = Arrays.stream(extendsWith.value()).
                filter(ConfigParameterResolver.class::isAssignableFrom).
                count();

        if (matched==0) {
            return false;
        } else if (matched==1) {
            return true;
        } else {
            throw new RuntimeException("Unexpected number of resolvers " + matched + " for " + klass.getCanonicalName());
        }
    }
}
