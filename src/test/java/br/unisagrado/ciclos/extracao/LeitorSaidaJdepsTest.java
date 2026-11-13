package br.unisagrado.ciclos.extracao;

import br.unisagrado.ciclos.extracao.DependenciasDeClasses.Dependencia;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeitorSaidaJdepsTest {

    /** Trecho no formato real do jdeps -verbose:class. */
    static final String EXEMPLO_DE_SAIDA = """
            app.jar -> java.base
            app.jar -> not found
               com.app.web.Controller          -> com.app.service.Service      app.jar
               com.app.web.Controller$1        -> com.app.service.Service      app.jar
               com.app.web.Controller          -> java.lang.String             java.base
               com.app.service.Service         -> com.app.web.Controller$Dto   app.jar
               com.app.service.Service         -> org.slf4j.Logger             not found
               com.app.service.Service$Interna -> com.app.service.Service      app.jar
               com.app.package-info            -> java.lang.Object             java.base
            """;

    @Test
    void agrupaClassesInternasEDescartaDependenciasExternas() {
        DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler(EXEMPLO_DE_SAIDA);

        assertEquals(List.of("com.app.service.Service", "com.app.web.Controller"),
                List.copyOf(dependencias.classes()));
        // Controller → Service (2x: Controller e Controller$1) e Service → Controller (via Controller$Dto).
        // Ficam de fora: String e Logger (externas), Service$Interna → Service (mesma classe) e package-info.
        assertEquals(List.of(
                        new Dependencia("com.app.web.Controller", "com.app.service.Service"),
                        new Dependencia("com.app.web.Controller", "com.app.service.Service"),
                        new Dependencia("com.app.service.Service", "com.app.web.Controller")),
                dependencias.dependencias());
    }

    @Test
    void mantemOCifraoQueFazParteDoNomeDaClasse() {
        // no Gson, $Gson$Types é uma classe comum, e não uma classe interna de "$Gson"
        DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler("""
                   g.Gson                                -> g.internal.$Gson$Types          gson.jar
                   g.internal.$Gson$Types                -> g.Gson                          gson.jar
                   g.internal.$Gson$Types$WildcardImpl   -> g.internal.$Gson$Types          gson.jar
                """);

        assertEquals(List.of("g.Gson", "g.internal.$Gson$Types"), List.copyOf(dependencias.classes()));
        assertEquals(List.of(
                        new Dependencia("g.Gson", "g.internal.$Gson$Types"),
                        new Dependencia("g.internal.$Gson$Types", "g.Gson")),
                dependencias.dependencias());
    }

    @Test
    void saidaSemDependenciasGeraConjuntoVazio() {
        DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler("vazio.jar -> java.base\n");

        assertEquals(0, dependencias.classes().size());
        assertEquals(0, dependencias.dependencias().size());
    }
}
