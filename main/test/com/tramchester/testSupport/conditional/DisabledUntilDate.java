package com.tramchester.testSupport.conditional;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledUntilDateCondition.class)
public @interface DisabledUntilDate  {
    int year();
    int month();
    int day() default 1;
}
