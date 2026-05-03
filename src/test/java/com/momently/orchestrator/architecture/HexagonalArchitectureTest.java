package com.momently.orchestrator.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 육각형 아키텍처 경계를 ArchUnit 규칙으로 검증한다.
 */
@AnalyzeClasses(
    packages = "com.momently.orchestrator",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    /**
     * 도메인은 애플리케이션·어댑터·설정 패키지에 의존하면 안 된다.
     */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..", "..config..");

    /**
     * 애플리케이션은 어댑터 구현체 패키지에 직접 의존하면 안 된다.
     */
    @ArchTest
    static final ArchRule application_must_not_depend_on_adapter_implementations =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter.in..", "..adapter.out..");
}
