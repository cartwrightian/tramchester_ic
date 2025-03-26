package com.tramchester.testSupport;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.lang.reflect.Method;

public class FindMethodLineNumber {

    private final ClassPool classPool;

    public FindMethodLineNumber() {
        classPool = ClassPool.getDefault();
    }

    public int findFor(final Method method) throws NotFoundException {
        CtClass ctClass = classPool.get(method.getDeclaringClass().getCanonicalName());

        CtMethod declaration = ctClass.getDeclaredMethod(method.getName());

        return declaration.getMethodInfo().getLineNumber(0);
    }
}
