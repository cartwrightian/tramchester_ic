package com.tramchester.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.tramchester.graph")
public class TestForNeo4jDirectDependency {

    @ArchTest
    static final ArchRule shouldNotHaveDirectDependencyOnNeo4JOutsideOfGraphPackage =
        layeredArchitecture()
                .consideringAllDependencies()
                //.layer("Repository").definedBy("..respository..")
                .layer("Graph").definedBy("com.tramchester.graph")
                .layer("Caches").definedBy("com.tramchester.graph.caches")
                .layer("Facade").definedBy("com.tramchester.graph.facade")
                .layer("Neo4J").definedBy("com.tramchester.graph.facade.neo4j")


                .whereLayer("Neo4J").mayOnlyBeAccessedByLayers("Facade")
                .whereLayer("Facade").mayOnlyBeAccessedByLayers("Graph", "Caches");

}
