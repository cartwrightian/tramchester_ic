package com.tramchester.integration.livedata;


import com.tramchester.livedata.tfgm.LiveDataFileFetcher;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LiveDataFileFetcherTest {

    @Test
    void shouldLoadDataInFile() {
        LiveDataFileFetcher fetcher = new LiveDataFileFetcher(TestEnv.LiveDataExampleFile);
        String data = fetcher.getData();
        Assertions.assertTrue(data.length()>0);
    }
}
