package com.tramchester.testSupport.conditional;

import com.tramchester.domain.dates.TramDate;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class DisabledUntilDateTestFinder {

    private final String packageName;
    private final ClassPool classPool = ClassPool.getDefault();


    public DisabledUntilDateTestFinder(String packageName) {
        this.packageName = packageName;
    }

    public @NotNull Set<Method> getExpiredMethods(final TramDate date) {
        Set<Method> havePath = getMethodsWithAnnotation();

        return havePath.stream().
                filter(method -> expired(date, method)).
                collect(Collectors.toSet());
    }

    public Set<Class<?>> getExpiredClasses(final TramDate date) {
       Set<Class<?>> haveAnnotation = getClassesWithAnnotation();

       return haveAnnotation.stream().filter(klass -> expired(date, klass)).collect(Collectors.toSet());
    }

    public boolean expired(final TramDate date, final AnnotatedElement element) {
        Optional<DisabledUntilDate> search = DisabledUntilDateCondition.findAnnotationFor(element);
        if (search.isPresent()) {
            DisabledUntilDate disabledUntilDate = search.get();

            TramDate annotationDate = TramDate.of(disabledUntilDate.year(), disabledUntilDate.month(), disabledUntilDate.day());
            return annotationDate.isBefore(date);
        } else {
            return false;
        }
    }

    public Set<Method> getMethodsWithAnnotation() {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.
                forPackages(packageName).
                addScanners(Scanners.MethodsAnnotated);

        Reflections reflections = new Reflections(builder);

        return reflections.getMethodsAnnotatedWith(Test.class).stream().
                filter(method -> DisabledUntilDateCondition.findAnnotationFor(method).isPresent()).
                collect(Collectors.toSet());
    }


    private Set<Class<?>> getClassesWithAnnotation() {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.forPackages(packageName).addScanners(Scanners.TypesAnnotated);

        Reflections reflections = new Reflections(builder);

        final Set<Class<?>> typesAndAnnotations = reflections.getTypesAnnotatedWith(DisabledUntilDate.class, true);

        // directly annotated classes
        final Set<Class<?>> classes = typesAndAnnotations.stream().filter(item -> !item.isAnnotation()).collect(Collectors.toSet());

        // annotations, themselves annotated with DisabledUntilDate
        final Set<Class<?>> additionalAnnotations = typesAndAnnotations.stream().
                filter(Class::isAnnotation).
                collect(Collectors.toSet());

        // those classes annotated with one of the derived annotations
        final Set<Class<?>> additional = additionalAnnotations.stream().
                flatMap(annotation -> reflections.getTypesAnnotatedWith((Class<? extends Annotation>) annotation).stream()).
                collect(Collectors.toSet());

        final HashSet<Class<?>> result = new HashSet<>(classes);
        result.addAll(additional);

        return result;
    }

    public String getMethodNamesAndLineNumbers(final Collection<Method> methods) throws NotFoundException {

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
                    append(findLineNumberOfAnnotationFor(method)).
                    append(')');
        }

        return  "Failed " + text;
    }



    public @Nullable String getClassNamesAndLineNumbers(List<Class<?>> list) {
        StringBuilder text = new StringBuilder();

        // TODO Find real line number of the declaration

        for(Class<?> klass : list) {
            //final Class<?> declaringClass = method.getDeclaringClass();

            // output in a format that allows click through direct to the methods in intellij
            if (!text.isEmpty()) {
                text.append(System.lineSeparator());
            }
            text.append(klass.getName()).
                    append('(').
                    append(klass.getSimpleName()).append(".java:0").
                    append(')');
        }

        return  "Failed " + text;
    }


    public int findLineNumberOfAnnotationFor(final Method method) throws NotFoundException {
        CtClass ctClass = classPool.get(method.getDeclaringClass().getCanonicalName());

        CtMethod declaration = ctClass.getDeclaredMethod(method.getName());

        return declaration.getMethodInfo().getLineNumber(0);
    }

}
