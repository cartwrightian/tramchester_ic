package com.tramchester.annotations;

import com.tramchester.domain.collections.ImmutableEnumSet;

import javax.annotation.meta.TypeQualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE_USE})
@TypeQualifier(applicableTo = ImmutableEnumSet.class)
public @interface Cached {

}
