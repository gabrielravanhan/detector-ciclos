package br.unisagrado.ciclos.extracao;

import br.unisagrado.ciclos.grafo.Digrafo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MontadorDeGrafosTest {

    private final DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler(LeitorSaidaJdepsTest.EXEMPLO_DE_SAIDA);

    @Test
    void grafoDeClassesSomaReferenciasRepetidas() {
        Digrafo classes = MontadorDeGrafos.grafoDeClasses(dependencias);

        int controller = classes.indiceDe("com.app.web.Controller");
        int service = classes.indiceDe("com.app.service.Service");
        assertEquals(2, classes.quantidadeVertices());
        assertEquals(2, classes.peso(controller, service), "Controller e Controller$1 usam Service");
        assertEquals(1, classes.peso(service, controller));
    }

    @Test
    void grafoDePacotesContaParesDeClasses() {
        Digrafo pacotes = MontadorDeGrafos.grafoDePacotes(MontadorDeGrafos.grafoDeClasses(dependencias));

        int web = pacotes.indiceDe("com.app.web");
        int service = pacotes.indiceDe("com.app.service");
        assertEquals(2, pacotes.quantidadeVertices());
        assertEquals(1, pacotes.peso(web, service), "um par de classes: Controller → Service");
        assertEquals(1, pacotes.peso(service, web));
    }

    @Test
    void dependenciaDentroDoMesmoPacoteNaoViraArestaEntrePacotes() {
        DependenciasDeClasses mesmoPacote = LeitorSaidaJdeps.ler("""
                   com.app.A   -> com.app.B   app.jar
                   com.app.B   -> com.app.A   app.jar
                """);

        Digrafo pacotes = MontadorDeGrafos.grafoDePacotes(MontadorDeGrafos.grafoDeClasses(mesmoPacote));

        assertEquals(1, pacotes.quantidadeVertices());
        assertEquals(0, pacotes.quantidadeArestas());
    }

    @Test
    void pacoteDeUmaClasse() {
        assertEquals("com.loja.web", MontadorDeGrafos.pacoteDe("com.loja.web.Controller"));
        assertEquals(MontadorDeGrafos.PACOTE_PADRAO, MontadorDeGrafos.pacoteDe("SemPacote"));
    }
}
