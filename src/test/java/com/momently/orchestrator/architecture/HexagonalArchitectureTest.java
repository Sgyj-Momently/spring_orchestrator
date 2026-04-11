package com.momently.orchestrator.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules proving the hexagonal architecture boundaries.
 */
@AnalyzeClasses(
    packages = "com.momently.orchestrator",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    /**
     * Domain must not depend on application, adapters, or config packages.
     */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..", "..config..");

    /**
     * Application must not depend directly on adapter implementations.
     */
    @ArchTest
    static final ArchRule application_must_not_depend_on_adapter_implementations =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter.in..", "..adapter.out..");
}
