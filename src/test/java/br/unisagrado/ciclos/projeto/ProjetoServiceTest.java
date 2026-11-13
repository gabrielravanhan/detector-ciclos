package br.unisagrado.ciclos.projeto;

import br.unisagrado.ciclos.common.exception.NaoEncontradoException;
import br.unisagrado.ciclos.extracao.LeitorSaidaJdeps;
import br.unisagrado.ciclos.extracao.MontadorDeGrafos;
import br.unisagrado.ciclos.grafo.Digrafo;
import br.unisagrado.ciclos.instancia.InstanciaService;
import br.unisagrado.ciclos.projeto.dto.response.CicloResponse;
import br.unisagrado.ciclos.projeto.dto.response.GrafoResponse;
import br.unisagrado.ciclos.projeto.dto.response.MalgrangeResponse;
import br.unisagrado.ciclos.projeto.dto.response.NivelResponse;
import br.unisagrado.ciclos.projeto.dto.response.PassoResponse;
import br.unisagrado.ciclos.projeto.dto.response.ProjetoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa o service direto, sem subir o Spring. */
class ProjetoServiceTest {

    private final ProjetoRepository projetoRepository = new ProjetoRepository();
    private final ProjetoService projetoService =
            new ProjetoService(projetoRepository, new InstanciaService(Path.of("pasta-inexistente")));

    private final String id = projetoService.carregarArestas("""
            A -> B
            B -> C
            C -> A
            B -> D
            D -> E
            E -> F
            F -> D
            """, "teste").id();

    @Test
    void codigoCompiladoGeraGrafosDePacotesEDeClasses() {
        ProjetoResponse projeto = projetoService.carregarJar(Path.of("target/classes"), "próprio projeto");

        assertEquals(List.of(Projeto.NIVEL_PACOTES, Projeto.NIVEL_CLASSES),
                projeto.niveis().stream().map(NivelResponse::nivel).toList());
        assertEquals("jar", projeto.origem());
        GrafoResponse classes = projetoService.buscarGrafo(projeto.id(), Projeto.NIVEL_CLASSES);
        assertTrue(classes.vertices().stream().anyMatch(v -> v.id().equals("br.unisagrado.ciclos.algoritmo.Malgrange")));
    }

    @Test
    void pastaSemClassesEhRecusada(@TempDir Path pastaVazia) {
        assertThrows(IllegalArgumentException.class, () -> projetoService.carregarJar(pastaVazia, "vazia"));
    }

    @Test
    void listaDeArestasGeraUmUnicoNivel() {
        ProjetoResponse projeto = projetoService.carregarArestas("a -> b", null);

        assertEquals(List.of(Projeto.NIVEL_INFORMADO), projeto.niveis().stream().map(NivelResponse::nivel).toList());
        assertEquals("grafo manual", projeto.nome(), "nome padrão quando nenhum é informado");
    }

    @Test
    void malgrangeDevolveComponentesEPassos() {
        MalgrangeResponse resposta = projetoService.executarMalgrange(id, null, true);

        assertEquals(Projeto.NIVEL_INFORMADO, resposta.nivel());
        assertEquals(2, resposta.metricas().componentesComCiclo());
        assertEquals(2, resposta.passos().size());
        PassoResponse primeiro = resposta.passos().get(0);
        assertEquals("A", primeiro.pivo());
        assertEquals(List.of("A"), primeiro.niveisSucessores().get(0));
        assertNull(resposta.observacaoPassos());
    }

    @Test
    void passosSaoOmitidosEmGrafosGrandes() {
        StringBuilder cadeia = new StringBuilder();
        for (int i = 0; i <= ProjetoService.LIMITE_PARA_REGISTRAR_PASSOS; i++) {
            cadeia.append("v").append(i).append(" -> v").append(i + 1).append('\n');
        }
        String grande = projetoService.carregarArestas(cadeia.toString(), "grande").id();

        MalgrangeResponse resposta = projetoService.executarMalgrange(grande, null, true);

        assertTrue(resposta.passos().isEmpty());
        assertNotNull(resposta.observacaoPassos());
    }

    @Test
    void grafoDevolveVerticesEArestasPeloNome() {
        GrafoResponse grafo = projetoService.buscarGrafo(id, null);

        assertEquals(6, grafo.vertices().size());
        assertEquals(7, grafo.arestas().size());
        assertEquals("A", grafo.arestas().get(0).origem());
    }

    @Test
    void cicloSegueAsArestasDoGrafo() {
        projetoService.executarMalgrange(id, null, false);
        CicloResponse ciclo = projetoService.buscarCiclo(id, null, 1, null);

        assertEquals(List.of("D", "E", "F", "D"), ciclo.ciclo());
        assertEquals(3, ciclo.arestas().size());
    }

    @Test
    void condensadoTemUmVerticePorComponente() {
        assertEquals(2, projetoService.buscarCondensacao(id, null).vertices().size());
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
        String comCiclo = projetoRepository.criar("app", "jar", grafos).id();

        projetoService.executarMalgrange(comCiclo, Projeto.NIVEL_PACOTES, false);
        CicloResponse ciclo = projetoService.buscarCiclo(comCiclo, Projeto.NIVEL_PACOTES, 0, null);

        assertEquals(List.of("com.app.service", "com.app.web", "com.app.service"), ciclo.ciclo());
        assertEquals(List.of("com.app.service.Service -> com.app.web.Formatador"),
                ciclo.arestas().get(0).dependenciasDeClasses());
    }

    @Test
    void pedidosInvalidosSaoRecusados() {
        assertThrows(IllegalArgumentException.class, () -> projetoService.buscarGrafo(id, "classes"));
        assertThrows(NaoEncontradoException.class, () -> projetoService.buscarCiclo(id, null, 99, null));
        assertThrows(IllegalArgumentException.class, () -> projetoService.buscarCiclo(id, null, 0, "D"));
        assertThrows(NaoEncontradoException.class, () -> projetoService.buscarGrafo("g999", null));
        assertThrows(IllegalArgumentException.class, () -> projetoService.carregarArquivo("foto.png", null));
    }
}
