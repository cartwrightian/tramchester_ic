package com.tramchester.domain.time;

import java.io.Serial;

public class InvalidDurationException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidDurationException(String message) {
        super(message);
    }
}
