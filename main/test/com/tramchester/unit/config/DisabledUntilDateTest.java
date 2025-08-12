package com.tramchester.unit.config;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.testSupport.FindMethodLineNumber;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import com.tramchester.testSupport.conditional.DisabledUntilDateCondition;
import javassist.NotFoundException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DisabledUntilDateTest {

    private TramDate today;
    private FindMethodLineNumber findMethodLineNumber;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        today = TramDate.from(TestEnv.LocalNow());
        findMethodLineNumber = new FindMethodLineNumber();
    }

    @Test
    void shouldNotHaveAnyExpiredDisabledTests() throws NotFoundException {

        Set<Method> all = getExpired(App.class.getPackageName(), today);

        List<Method> expired = all.stream().
                filter(method -> !method.getDeclaringClass().equals(this.getClass())).
                sorted(Comparator.comparing(a -> a.getDeclaringClass().getName())).
                toList();

        assertEquals(Collections.emptyList(), expired, getMethodNamesAndLineNumbers(expired));
    }

    @DisabledUntilDate(year = 2010, month = 11, day = 30)
    @Test
    void shouldCheckWeFindActualExpiredAnnotation() {
        String packageName = DisabledUntilDateTest.class.getPackageName();

        Set<Method> expired = getExpired(packageName, today);

        assertFalse(expired.isEmpty());

        List<Method> byThisClass = expired.stream().
                filter(method -> method.getDeclaringClass().equals(DisabledUntilDateTest.class)).
                toList();

        assertEquals(2, byThisClass.size());
    }

    @Test
    @AnnotatedClassTestThatHasExpired
    void shouldMatchCustomAnnotationExpired() throws NoSuchMethodException {
        Method currentMethod = this.getClass().getDeclaredMethod("shouldMatchCustomAnnotationExpired");
        assertNotNull(currentMethod);

        boolean expired = expired(today, currentMethod);
        assertTrue(expired);
    }

    @Test
    @AnnotatedClassTestThatHasNotExpired
    void shouldAlwaysBeDisabled() {
        fail("should be disabled");
    }

    @Test
    void shouldMatchCustomAnnotationNotExpired() throws NoSuchMethodException {
        Method disabled = this.getClass().getDeclaredMethod("shouldAlwaysBeDisabled");
        assertNotNull(disabled);

        boolean expired = expired(today, disabled);
        assertFalse(expired);
    }

    @Test
    void shouldFindAnnotationForMethodInThisClass() {
        String packageName = DisabledUntilDateTest.class.getPackageName();

        Set<Method> methods = getMethodsWithAnnotation(packageName);

        assertFalse(methods.isEmpty());

        List<Method> byThisClass = methods.stream().
                filter(method -> method.getDeclaringClass().equals(DisabledUntilDateTest.class)).
                toList();

        assertEquals(3, byThisClass.size(), byThisClass.toString());

    }

    private static Set<Method> getMethodsWithAnnotation(final String packageName) {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.
                forPackages(packageName).
                addScanners(Scanners.MethodsAnnotated);

        Reflections reflections = new Reflections(builder);

        return reflections.getMethodsAnnotatedWith(Test.class).stream().
                filter(method -> findAnnotationFor(method).isPresent()).
                collect(Collectors.toSet());
    }

    private static Optional<DisabledUntilDate> findAnnotationFor(Method method) {
        return DisabledUntilDateCondition.findAnnotationFor(method);
    }

    private @NotNull Set<Method> getExpired(final String packageName, final TramDate date) {
        Set<Method> havePath = getMethodsWithAnnotation(packageName);

        return havePath.stream().
                filter(method -> expired(date, method)).
                collect(Collectors.toSet());
    }

    private boolean expired(final TramDate date, final Method method) {
        Optional<DisabledUntilDate> search = findAnnotationFor(method);
        if (search.isPresent()) {
            DisabledUntilDate disabledUntilDate = search.get();

            TramDate annotationDate = TramDate.of(disabledUntilDate.year(), disabledUntilDate.month(), disabledUntilDate.day());
            return annotationDate.isBefore(date);
        } else {
            return false;
        }
    }

    private String getMethodNamesAndLineNumbers(final Collection<Method> methods) throws NotFoundException {

        StringBuilder text = new StringBuilder();

        for(Method method : methods) {
            final Class<?> declaringClass = method.getDeclaringClass();

            // output in a format that allows click through direct to the methods in intellij
            if (!text.isEmpty()) {
                text.append(System.lineSeparator());
            }
            text.append(declaringClass.getName()).append(method.getName()).
                    append('(').
                    append(declaringClass.getSimpleName()).append(".java:").
                    append(findMethodLineNumber.findFor(method)).
                    append(')');
        }

        return  "Failed " + text;
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @DisabledUntilDate(year = 2012, month = 10, day = 30)
    public @interface AnnotatedClassTestThatHasExpired {
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @DisabledUntilDate(year = 2099, month = 10, day = 30)
    public @interface AnnotatedClassTestThatHasNotExpired {
    }
}
