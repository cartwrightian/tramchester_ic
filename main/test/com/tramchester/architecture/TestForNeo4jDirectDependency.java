package com.tramchester.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = { "com.tramchester.graph", "org.neo4j.graphdb" })
public class TestForNeo4jDirectDependency {

    @ArchTest
    static final ArchRule shouldNotHaveDirectDependencyOnNeo4JOutsideOfGraphPackage =
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Graph").definedBy("com.tramchester.graph")
                .layer("Caches").definedBy("com.tramchester.graph.caches")
                .layer("Search").definedBy("com.tramchester.graph.search")
                .layer("Neo4JSearch").definedBy("com.tramchester.graph.search.neo4j..")
                .layer("DTODiag").definedBy("com.tramchester.domain.presentation.DTO.diagnostics")
                .layer("Build").definedBy("com.tramchester.graph.graphbuild..")
                .layer("Core").definedBy("com.tramchester.graph.core")
                .layer("StateMachine").definedBy("com.tramchester.graph.search.stateMachine..")

                .layer("Neo4JCore").definedBy("com.tramchester.graph.core.neo4j")
                .layer("Neo4JImplementation").definedBy("org.neo4j..")

                .whereLayer("Neo4JImplementation").mayOnlyBeAccessedByLayers("Neo4JCore", "Neo4JSearch")
                .whereLayer("Neo4JCore").mayOnlyBeAccessedByLayers("Core", "Neo4JSearch")
                .whereLayer("Neo4JSearch").mayOnlyBeAccessedByLayers("Search")
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Graph", "Caches", "Neo4JCore", "Search",
                        "DTODiag", "Build", "Neo4JSearch", "StateMachine");

}
