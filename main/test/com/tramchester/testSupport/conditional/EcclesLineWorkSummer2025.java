package com.tramchester.testSupport.conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@DisabledUntilDate(year = 2025, month = 8, day = 11)
public @interface EcclesLineWorkSummer2025 {
}
