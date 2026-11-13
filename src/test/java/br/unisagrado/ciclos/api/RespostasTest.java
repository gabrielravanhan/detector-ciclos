package br.unisagrado.ciclos.api;

import br.unisagrado.ciclos.api.ModelosJson.CicloJson;
import br.unisagrado.ciclos.api.ModelosJson.GrafoJson;
import br.unisagrado.ciclos.api.ModelosJson.PassoJson;
import br.unisagrado.ciclos.api.ModelosJson.ResultadoMalgrangeJson;
import br.unisagrado.ciclos.extracao.LeitorSaidaJdeps;
import br.unisagrado.ciclos.extracao.MontadorDeGrafos;
import br.unisagrado.ciclos.grafo.Digrafo;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespostasTest {

    private final RepositorioDeProjetos repositorio = new RepositorioDeProjetos();
    private final Projeto projeto = repositorio.carregarListaDeArestas("""
            A -> B
            B -> C
            C -> A
            B -> D
            D -> E
            E -> F
            F -> D
            """, "teste");

    @Test
    void malgrangeDevolveComponentesEPassos() {
        ResultadoMalgrangeJson resposta = Respostas.malgrange(projeto, null, true);

        assertEquals(Projeto.NIVEL_INFORMADO, resposta.nivel());
        assertEquals(2, resposta.metricas().componentesComCiclo());
        assertEquals(2, resposta.passos().size());
        PassoJson primeiro = resposta.passos().get(0);
        assertEquals("A", primeiro.pivo());
        assertEquals(List.of("A"), primeiro.niveisSucessores().get(0));
        assertNull(resposta.observacaoPassos());
    }

    @Test
    void passosSaoOmitidosEmGrafosGrandes() {
        StringBuilder cadeia = new StringBuilder();
        for (int i = 0; i <= Respostas.LIMITE_PARA_REGISTRAR_PASSOS; i++) {
            cadeia.append("v").append(i).append(" -> v").append(i + 1).append('\n');
        }
        Projeto grande = repositorio.carregarListaDeArestas(cadeia.toString(), "grande");

        ResultadoMalgrangeJson resposta = Respostas.malgrange(grande, null, true);

        assertTrue(resposta.passos().isEmpty());
        assertNotNull(resposta.observacaoPassos());
    }

    @Test
    void grafoDevolveVerticesEArestasPeloNome() {
        GrafoJson grafo = Respostas.grafo(projeto, null);

        assertEquals(6, grafo.vertices().size());
        assertEquals(7, grafo.arestas().size());
        assertEquals("A", grafo.arestas().get(0).origem());
    }

    @Test
    void cicloSegueAsArestasDoGrafo() {
        Respostas.malgrange(projeto, null, false);
        CicloJson ciclo = Respostas.ciclo(projeto, null, 1, null);

        assertEquals(List.of("D", "E", "F", "D"), ciclo.ciclo());
        assertEquals(3, ciclo.arestas().size());
    }

    @Test
    void condensadoTemUmVerticePorComponente() {
        assertEquals(2, Respostas.condensacao(projeto, null).vertices().size());
    }

    @Test
    void evidenciaListaAsClassesQueCriamOCicloEntrePacotes() {
        Digrafo classes = MontadorDeGrafos.grafoDeClasses(LeitorSaidaJdeps.ler("""
                   com.app.web.Controller    -> com.app.service.Service   app.jar
                   com.app.service.Service   -> com.app.web.Formatador    app.jar
                   com.app.web.Formatador    -> java.lang.String          java.base
                """));
        Map<String, Digrafo> grafos = new LinkedHashMap<>();
        grafos.put(Projeto.NIVEL_PACOTES, MontadorDeGrafos.grafoDePacotes(classes));
        grafos.put(Projeto.NIVEL_CLASSES, classes);
        Projeto comCiclo = new Projeto("p1", "app", "jar", grafos);

        Respostas.malgrange(comCiclo, Projeto.NIVEL_PACOTES, false);
        CicloJson ciclo = Respostas.ciclo(comCiclo, Projeto.NIVEL_PACOTES, 0, null);

        assertEquals(List.of("com.app.service", "com.app.web", "com.app.service"), ciclo.ciclo());
        assertEquals(List.of("com.app.service.Service -> com.app.web.Formatador"),
                ciclo.arestas().get(0).dependenciasDeClasses());
    }

    @Test
    void pedidosInvalidosSaoRecusados() {
        assertThrows(IllegalArgumentException.class, () -> Respostas.grafo(projeto, "classes"));
        assertThrows(NoSuchElementException.class, () -> Respostas.ciclo(projeto, null, 99, null));
        assertThrows(IllegalArgumentException.class, () -> Respostas.ciclo(projeto, null, 0, "D"));
    }
}
