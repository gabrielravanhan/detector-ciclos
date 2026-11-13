package br.unisagrado.ciclos.analise;

import br.unisagrado.ciclos.algoritmo.Malgrange;
import br.unisagrado.ciclos.grafo.Digrafo;
import br.unisagrado.ciclos.grafo.ListaDeArestas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CondensacaoTest {

    /** Exemplo de sala sem o vértice isolado: CFCs {A, B, C}, {D, E, F} e {G, H}. */
    private final Digrafo grafo = ListaDeArestas.ler("""
            A -> B
            B -> C
            C -> A
            B -> D
            D -> E
            E -> F
            F -> D
            G -> F
            G -> H
            H -> G
            """);
    private final List<List<Integer>> componentes = Malgrange.executar(grafo, false).componentes();

    @Test
    void condensadoEhAciclicoEEstaEmOrdemTopologica() {
        Condensacao condensacao = Condensacao.construir(grafo, componentes);

        // {A,B,C} → {D,E,F} e {G,H} → {D,E,F}
        assertEquals(3, condensacao.ordemTopologica().size());
        assertEquals(2, condensacao.arestas().size());
        List<Integer> ordem = condensacao.ordemTopologica();
        for (Condensacao.Aresta aresta : condensacao.arestas()) {
            assertTrue(ordem.indexOf(aresta.origem()) < ordem.indexOf(aresta.destino()),
                    "aresta " + aresta + " fora de ordem");
        }
    }

    @Test
    void condensadoSomaOsPesosEntreComponentes() {
        Digrafo comPesos = ListaDeArestas.ler("a -> b 2\nb -> a\na -> c 3\nb -> c 4");
        List<List<Integer>> cfcs = Malgrange.executar(comPesos, false).componentes();

        Condensacao condensacao = Condensacao.construir(comPesos, cfcs);

        // {a, b} → {c} com peso 3 + 4
        assertEquals(List.of(new Condensacao.Aresta(0, 1, 7)), condensacao.arestas());
        assertEquals(0, condensacao.camada()[0]);
        assertEquals(1, condensacao.camada()[1]);
    }

    @Test
    void condensadoRecusaComponentesQueNaoSaoCfcs() {
        // A e B separados de C: C → A e B → C formariam ciclo no condensado
        List<List<Integer>> erradas = List.of(List.of(0, 1), List.of(2), List.of(3, 4, 5), List.of(6, 7));

        assertThrows(IllegalStateException.class, () -> Condensacao.construir(grafo, erradas));
    }
}
