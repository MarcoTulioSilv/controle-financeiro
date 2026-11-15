package com.marcotulio.financeiro;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

/**
 * Verifica as regras de organização em módulos (docs/design/modulos.md).
 */
class ArquiteturaTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.marcotulio.financeiro");

    /** M1: nenhum ciclo de dependência entre módulos. */
    @Test
    void modulosNaoTemCiclos() {
        slices().matching("com.marcotulio.financeiro.(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    /** M2: o interpretador é uma função pura, sem Spring nem JPA. */
    @Test
    void interpretadorNaoDependeDeSpringNemDeJpa() {
        noClasses().that().resideInAPackage("..interpretador..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    /** M5: entidades ficam visíveis só no próprio módulo; os outros usam o serviço público. */
    @Test
    void entidadesNaoSaoPublicas() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().notBePublic()
                .check(CLASSES);
    }

    /** M5: ninguém grava dados de outro módulo pelo repositório, contornando as regras dele. */
    @Test
    void repositoriosNaoSaoPublicos() {
        classes().that().areAssignableTo(Repository.class)
                .should().notBePublic()
                .check(CLASSES);
    }
}