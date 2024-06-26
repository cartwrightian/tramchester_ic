package com.tramchester.dataimport.loader;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.RailTransportDataFromFiles;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.repository.TransportDataContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@LazySingleton
public class DirectDataSourceFactory implements Iterable<DirectDataSourceFactory.PopulatesContainer> {
    private static final Logger logger = LoggerFactory.getLogger(DirectDataSourceFactory.class);

    private final List<PopulatesContainer> dataSources;
    private final RailTransportDataFromFiles loadRail;

    @Inject
    public DirectDataSourceFactory(RailTransportDataFromFiles loadRail) {
        this.loadRail = loadRail;
        dataSources = new LinkedList<>();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (loadRail.isEnabled()) {
            logger.info("Add rail");
            dataSources.add(loadRail);
        }
        logger.info("started");
    }

    @NotNull
    @Override
    public Iterator<DirectDataSourceFactory.PopulatesContainer> iterator() {
        return dataSources.iterator();
    }

    public interface PopulatesContainer {
        void loadInto(TransportDataContainer dataContainer);
        DataSourceInfo getDataSourceInfo();
    }

}
