package com.tramchester.integration.testSupport.config;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.TestGroupType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Train;
import static java.lang.String.format;

public class GraphDBTestConfig implements GraphDBConfig {
    private final TestGroupType group;
    private final EnumSet<TransportMode> modesFromConfig;
    private final TramchesterConfig config;

    public GraphDBTestConfig(TestGroupType group, TramchesterConfig config) {
        this.group = group;
        this.modesFromConfig = config.getTransportModes();
        this.config = config;
    }

    public Path getDbPath() {
        return createGraphDatabasePath(group, config);
    }

    @Override
    public String getNeo4jPagecacheMemory() {
        if (this.modesFromConfig.contains(Bus)) {
            return "300m";
        }
        if (this.modesFromConfig.contains(Train)) {
            return "1000m";
        }
        return "100m";
    }

    @Override
    public String getMemoryTransactionGlobalMaxSize() {
        return "650m";
    }

    @Override
    public Boolean enableDiagnostics() {
        return false;
    }

    private Path createGraphDatabasePath(TestGroupType group, TramchesterConfig config) {
        Set<DataSourceID> sourcesFromConfig = config.getRemoteDataSourceConfig().stream().
                map(RemoteDataSourceConfig::getDataSourceId).
                filter(dataSourceId -> dataSourceId !=DataSourceID.database).
                collect(Collectors.toSet());

        List<GTFSSourceConfig> gtfsDataSource = config.getGTFSDataSource();
        Set<TransportMode> groupedModesFromConfig = gtfsDataSource.stream().
                flatMap(gtfsSourceConfig -> gtfsSourceConfig.groupedStationModes().stream()).collect(Collectors.toSet());

        boolean hasClosures = gtfsDataSource.stream().anyMatch(gtfsSourceConfig -> !gtfsSourceConfig.getStationClosures().isEmpty());

        String modes = modesFromConfig.isEmpty() ? "NoModesEnabled" : asText(modesFromConfig);

        String subFolderName = format("%s_%s", group, modes);

        if (!sourcesFromConfig.isEmpty()) {
            subFolderName = subFolderName + "_sources_" + asText(sourcesFromConfig);
        }

        String dbName = "tramchester_database";
        if (!groupedModesFromConfig.isEmpty()) {
            dbName = dbName + "_grouped_" + asText(groupedModesFromConfig);
        }
        if (config.hasNeighbourConfig()) {
            dbName = dbName + "_withNeighbours";
        }
        if (hasClosures) {
            dbName = dbName + closuresText(gtfsDataSource);
        }
        if (config.isGraphFiltered()) {
            // TODO proper naming
            dbName = dbName + "_isFiltered";
        }

        Path containingFolder = Path.of("databases", subFolderName);
        return containingFolder.resolve(dbName + ".db");
    }

    private static @NotNull String closuresText(final List<GTFSSourceConfig> gtfsDataSource) {
        // iff closures are in effect
        // then....
        final List<String> items = gtfsDataSource.stream().
                flatMap(gtfsConfig -> gtfsConfig.getStationClosures().stream()).
                map(GraphDBTestConfig::closuresText).toList();
        StringBuilder result = new StringBuilder();
        for(String text : items) {
            if (!result.isEmpty()) {
                result.append("_");
            }
            result.append(text);
        }
        return "_withClosures_"+result;
    }

    private static String closuresText(final StationClosures stationClosures) {

        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;
        final LocalDate startDate = stationClosures.getDateRange().getStartDate().toLocalDate();
        final LocalDate endDate = stationClosures.getDateRange().getEndDate().toLocalDate();
        final String ids = stationClosures.getStations().stream().
                map(IdFor::getGraphId).
                reduce("",(a, b) -> a + b);
        return String.format("%s_%s_%s", dateTimeFormatter.format(startDate), dateTimeFormatter.format(endDate), ids);
    }

    // need consistent ordering here to prevent duplication
    private <E extends Enum<E>> String asText(Set<E> enums) {

        List<E> ordered = enums.stream().sorted(Comparator.comparing(Enum::name)).toList();

        StringBuilder builder = new StringBuilder();
        for (E value : ordered) {
            if (!builder.isEmpty()) {
                builder.append("_");
            }
            builder.append(value.name());
        }
        return builder.toString();
    }

}
