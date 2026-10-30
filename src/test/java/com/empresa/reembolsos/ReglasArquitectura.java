package com.empresa.reembolsos;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.empresa.reembolsos")
public class ReglasArquitectura {

    // Regla 1: procesamiento no depende de infraestructura, adaptadores ni JPA
    @ArchTest
    static final ArchRule procesamientoSinPersistenciaNiWeb =
            noClasses().that().resideInAPackage("..procesamiento..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infraestructura..", "..adaptadores..",
                            "org.springframework.data..", "jakarta.persistence..");

    // Regla 2: el controlador solo accede al servicio de aplicacion, al dominio Y a si mismo
    @ArchTest
    static final ArchRule controladorSoloUsaServicio =
            classes().that().resideInAPackage("..adaptadores.rest..")
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                            "..aplicacion..", "..dominio..", 
                            "..adaptadores.rest..", // <-- Solucion al Error 2: Permitir su propio paquete
                            "org.springframework.web..", "org.springframework.http..", "java..");

    // Regla 3: las calculadoras implementan CalculadoraReembolso (excluyendo al Selector)
    @ArchTest
    static final ArchRule calculadorasImplementanInterfaz =
            classes().that().resideInAPackage("..procesamiento.calculo..")
                    .and().areNotInterfaces()
                    .and().doNotHaveSimpleName("SelectorCalculadora") // <-- Solucion al Error 1: Excluir el factory
                    .should().implement(com.empresa.reembolsos.procesamiento.calculo.CalculadoraReembolso.class);

    // Regla 4: los notificadores implementan ObservadorCambioEstado
    @ArchTest
    static final ArchRule notificadoresImplementanInterfaz =
            classes().that().resideInAPackage("..procesamiento.notificacion..")
                    .and().areNotInterfaces()
                    .should().implement(com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado.class);
}