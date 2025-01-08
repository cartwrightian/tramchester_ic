package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.emf.common.util.URI;

@LazySingleton
public class DoesPostRequest {
    public String post(URI uri, String body) {
        throw new NotImplementedException();
    }
}
