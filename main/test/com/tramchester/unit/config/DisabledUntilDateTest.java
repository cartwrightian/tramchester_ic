package com.tramchester.unit.config;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import com.tramchester.testSupport.conditional.DisabledUntilDateTestFinder;
import com.tramchester.unit.config.examples.ExampleDisabledForDateTest;
import com.tramchester.unit.config.examples.ExampleDisabledForDateUsingAnnotationTest;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DisabledUntilDateTest {

    private TramDate today;

    private final String examplesPackageName = ExampleDisabledForDateTest.class.getPackageName();


    @BeforeEach
    void onceBeforeEachTestRuns() {
        today = TramDate.from(TestEnv.LocalNow());
    }

    @Test
    void shouldNotHaveAnyExpiredTests() throws NotFoundException {

        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(App.class.getPackageName());

        Set<Method> all = finder.getExpiredMethods(today);

        List<Method> expired = all.stream().
                filter(method -> !method.getDeclaringClass().equals(this.getClass())).
                sorted(Comparator.comparing(a -> a.getDeclaringClass().getName())).
                toList();

        assertEquals(Collections.emptyList(), expired, finder.getMethodNamesAndLineNumbers(expired));
    }

    @Test
    void shouldNotHaveAnyExpiredTestsFixtures() {
        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(App.class.getPackageName());

        Set<Class<?>> all = finder.getExpiredClasses(today);

        List<Class<?>> expired = all.stream().
                //filter(klass -> !klass.getDeclaringClass().equals(this.getClass())).
                filter(klass -> !klass.getPackageName().equals(examplesPackageName)).
                sorted(Comparator.comparing(a -> a.getDeclaringClass().getName())).
                toList();

        assertEquals(Collections.emptyList(), expired, finder.getClassNamesAndLineNumbers(expired));

    }

    @Test
    void shouldFindExpiredTestFixtures() {
        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(examplesPackageName);

        Set<Class<?>> allExpired =  finder.getExpiredClasses(today);
        Set<Class<?>> expired = allExpired.stream().
                filter(klass -> klass.getPackageName().equals(examplesPackageName)).
                collect(Collectors.toSet());

        assertEquals(2, expired.size(), expired.toString());

        assertTrue(expired.contains(ExampleDisabledForDateTest.class));
        assertTrue(expired.contains(ExampleDisabledForDateUsingAnnotationTest.class));

    }

    @DisabledUntilDate(year = 2010, month = 11, day = 30)
    @Test
    void shouldCheckWeFindActualExpiredAnnotation() {
        String packageName = DisabledUntilDateTest.class.getPackageName();

        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(packageName);

        Set<Method> expired = finder.getExpiredMethods(today);

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

        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(DisabledUntilDateTest.class.getPackageName());

        boolean expired = finder.expired(today, currentMethod);
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

        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(DisabledUntilDateTest.class.getPackageName());

        boolean expired = finder.expired(today, disabled);
        assertFalse(expired);
    }

    @Test
    void shouldHaveUnExpiredExampleClass() {
        Class<?> klass = DisabledUntilDateTest.class;

        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(klass.getPackageName());

        assertFalse(finder.expired(today, klass));
    }

    @Test
    void shouldHaveExpiredExampleClass() {
        Class<ExampleDisabledForDateTest> klass = ExampleDisabledForDateTest.class;

        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(examplesPackageName);

        assertTrue(finder.expired(today, klass));

    }

    @Test
    void shouldFindAnnotationForMethodInThisClass() {
        String packageName = DisabledUntilDateTest.class.getPackageName();

        DisabledUntilDateTestFinder finder = new DisabledUntilDateTestFinder(packageName);

        Set<Method> methods = finder.getMethodsWithAnnotation();

        assertFalse(methods.isEmpty());

        List<Method> byThisClass = methods.stream().
                filter(method -> method.getDeclaringClass().equals(DisabledUntilDateTest.class)).
                toList();

        assertEquals(3, byThisClass.size(), byThisClass.toString());

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
