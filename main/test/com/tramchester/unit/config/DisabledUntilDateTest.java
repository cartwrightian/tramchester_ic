package com.tramchester.unit.config;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.testSupport.FindMethodLineNumber;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import javassist.NotFoundException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
                sorted((a,b) -> a.getDeclaringClass().getName().compareTo(b.getDeclaringClass().getName())).
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

        assertEquals(1, byThisClass.size());
    }

    @Test
    void shouldFindAnnotationForMethodInThisClass() {
        String packageName = DisabledUntilDateTest.class.getPackageName();

        Set<Method> methods = getMethodsWithAnnotation(packageName);

        assertFalse(methods.isEmpty());

        List<Method> byThisClass = methods.stream().
                filter(method -> method.getDeclaringClass().equals(DisabledUntilDateTest.class)).
                toList();

        assertEquals(1, byThisClass.size());

    }

    private static Set<Method> getMethodsWithAnnotation(final String packageName) {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.
                forPackages(packageName).
                addScanners(Scanners.MethodsAnnotated);

        Reflections reflections = new Reflections(builder);

        return reflections.getMethodsAnnotatedWith(DisabledUntilDate.class);
    }

    private @NotNull Set<Method> getExpired(String packageName, TramDate now) {
        Set<Method> havePath = getMethodsWithAnnotation(packageName);

        return havePath.stream().
                filter(method -> expired(now, method)).
                collect(Collectors.toSet());
    }

    private boolean expired(TramDate now, Method method) {
        DisabledUntilDate annotation = method.getAnnotation(DisabledUntilDate.class);
        if (annotation==null) {
            return false;
        }
        TramDate annotationDate = TramDate.of(annotation.year(), annotation.month(), annotation.day());
        return annotationDate.isBefore(now);
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
}
