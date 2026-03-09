package com.tramchester.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = { "com.tramchester" })
public class TestForGraphDatabaseDependency {

    @ArchTest
    static final ArchRule shouldHaveWellDefinedStructureForGraphDBAccess =
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Graph").definedBy("com.tramchester.graph")
                .layer("Caches").definedBy("com.tramchester.graph.caches")
                .layer("Search").definedBy("com.tramchester.graph.search")
                .layer("InMemorySearch").definedBy("com.tramchester.graph.search.inMemory")
                .layer("Diag").definedBy("com.tramchester.graph.search.diagnostics")
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


                .layer("UnitTest").definedBy("com.tramchester.unit..")
                .layer("UnitTestInMemory").definedBy("com.tramchester.unit.graph.inMemory")
                .layer("IntegrationTest").definedBy("com.tramchester.integration..")
                .layer("IntegrationTestInMemory").definedBy("com.tramchester.integration.graph.inMemory")

                .layer("TestSupport").definedBy("com.tramchester.testSupport")

                .whereLayer("InMemory").mayOnlyBeAccessedByLayers("InMemorySearch", "Modules", "UnitTest",
                       "InMemoryPersist", "IntegrationTestInMemory")
                .whereLayer("InMemoryPersist").mayOnlyBeAccessedByLayers("InMemory", "Core", "IntegrationTest", "UnitTestInMemory")
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Graph", "Caches", "Search",
                        "Build", "StateMachine", "Diag", "DBMgmt", "Resources", "DTODiag", "Healthchecks",
                        "Modules", "InMemory", "InMemorySearch", "UnitTest", "IntegrationTest", "TestSupport");

}
