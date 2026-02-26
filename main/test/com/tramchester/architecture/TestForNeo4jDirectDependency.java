package com.tramchester.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = { "com.tramchester", "org.neo4j.graphdb" })
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
                .layer("InMemoryPersist").definedBy("com.tramchester.graph.core.inMemory.persist")


                .layer("Neo4JCore").definedBy("com.tramchester.graph.core.neo4j")
                .layer("Neo4JImplementation").definedBy("org.neo4j..")

                .layer("UnitTest").definedBy("com.tramchester.unit..")
                .layer("UnitTestNeo4J").definedBy("com.tramchester.unit.graph.neo4J..")
                .layer("UnitTestInMemory").definedBy("com.tramchester.unit.graph.inMemory")
                .layer("IntegrationTest").definedBy("com.tramchester.integration..")
                .layer("IntegrationTestInMemory").definedBy("com.tramchester.integration.graph.inMemory")

                .layer("IntegrationTestNeo4J").definedBy("com.tramchester.integration.graph.neo4J..")
                .layer("Comparison").definedBy("com.tramchester.integration.graph.compare")

                .layer("TestSupport").definedBy("com.tramchester.testSupport")

                .whereLayer("Neo4JImplementation").mayOnlyBeAccessedByLayers("Neo4JCore", "Neo4JSearch",
                        "UnitTestNeo4J", "IntegrationTestNeo4J")
                .whereLayer("InMemory").mayOnlyBeAccessedByLayers("InMemorySearch", "Modules", "UnitTest",
                        "Comparison", "InMemoryPersist", "IntegrationTestInMemory")
                .whereLayer("Neo4JCore").mayOnlyBeAccessedByLayers("Neo4JSearch",  "Modules",
                        "UnitTestNeo4J", "IntegrationTestNeo4J", "Comparison")
                .whereLayer("Neo4JSearch").mayOnlyBeAccessedByLayers("Caches", "Modules", "IntegrationTestNeo4J",
                        "UnitTestNeo4J", "Comparison")
                .whereLayer("InMemoryPersist").mayOnlyBeAccessedByLayers("InMemory", "Core", "IntegrationTest", "UnitTestInMemory")
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Graph", "Caches", "Neo4JCore", "Search",
                        "Build", "Neo4JSearch", "StateMachine", "Diag", "DBMgmt", "Resources", "DTODiag", "Healthchecks",
                        "Modules", "InMemory", "InMemorySearch", "UnitTest", "IntegrationTest", "TestSupport");

}
