package com.tramchester.testSupport.conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@DisabledUntilDate(year = 2025, month = 5, day = 30)
public @interface BuryWorksSummer2025{
}
