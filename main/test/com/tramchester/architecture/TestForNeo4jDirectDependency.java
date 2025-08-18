package com.tramchester.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.regex.Pattern;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = { "com.tramchester", "org.neo4j.graphdb" }, importOptions = { TestForNeo4jDirectDependency.IgnoreTests.class })
public class TestForNeo4jDirectDependency {

    @ArchTest
    static final ArchRule shouldNotHaveDirectDependencyOnNeo4JOutsideOfGraphPackage =
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Graph").definedBy("com.tramchester.graph")
                .layer("Caches").definedBy("com.tramchester.graph.caches")
                .layer("Search").definedBy("com.tramchester.graph.search")
                .layer("InMemorySearch").definedBy("com.tramchester.graph.search.inMemory")
                .layer("Diag").definedBy("com.tramchester.graph.search.diagnostics")
                .layer("Neo4JSearch").definedBy("com.tramchester.graph.search.neo4j..")
                .layer("DTODiag").definedBy("com.tramchester.domain.presentation.DTO.diagnostics")
                .layer("Build").definedBy("com.tramchester.graph.graphbuild..")
                .layer("Core").definedBy("com.tramchester.graph.core")
                .layer("DBMgmt").definedBy("com.tramchester.graph.databaseManagement")
                .layer("StateMachine").definedBy("com.tramchester.graph.search.stateMachine..")
                .layer("Resources").definedBy("com.tramchester.resources")
                .layer("Healthchecks").definedBy("com.tramchester.healthchecks")
                .layer("Modules").definedBy("com.tramchester.modules")
                .layer("InMemory").definedBy("com.tramchester.graph.core.inMemory")

                .layer("Neo4JCore").definedBy("com.tramchester.graph.core.neo4j")
                .layer("Neo4JImplementation").definedBy("org.neo4j..")

                .whereLayer("Neo4JImplementation").mayOnlyBeAccessedByLayers("Neo4JCore", "Neo4JSearch")
                .whereLayer("InMemory").mayOnlyBeAccessedByLayers("InMemorySearch", "Modules")
                .whereLayer("Neo4JCore").mayOnlyBeAccessedByLayers("Neo4JSearch",  "Modules")
                .whereLayer("Neo4JSearch").mayOnlyBeAccessedByLayers("Caches", "Modules")
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Graph", "Caches", "Neo4JCore", "Search",
                        "Build", "Neo4JSearch", "StateMachine", "Diag", "DBMgmt", "Resources", "DTODiag", "Healthchecks",
                        "Modules", "InMemory", "InMemorySearch");

    static class IgnoreTests implements ImportOption {

        private final Pattern pattern = Pattern.compile(".*/test/com/tramchester/.*");

        @Override
        public boolean includes(Location location) {
            return !location.matches(pattern);
        }
    }
}
