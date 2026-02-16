package com.tramchester.config;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.matcher.StringMatcher;
import org.apache.commons.text.matcher.StringMatcherFactory;

import java.nio.file.Path;

public class FileVariableSubstitutor extends StringSubstitutor {

    public static final StringMatcher DEFAULT_PREFIX = StringMatcherFactory.INSTANCE.stringMatcher("%[");
    public static final StringMatcher DEFAULT_SUFFIX = StringMatcherFactory.INSTANCE.stringMatcher("]");
    private static final char DEFAULT_ESCAPE = '%';

    public FileVariableSubstitutor(final Path configDir) {
        super(new FilenameContentsLookUp(configDir),DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_ESCAPE);
        this.setEnableUndefinedVariableException(true);
        this.setEnableSubstitutionInVariables(true);
    }


}
