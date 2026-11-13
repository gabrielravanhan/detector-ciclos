package br.unisagrado.ciclos.analise;

import br.unisagrado.ciclos.algoritmo.Malgrange;
import br.unisagrado.ciclos.grafo.Digrafo;
import br.unisagrado.ciclos.grafo.ListaDeArestas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricasTest {

    @Test
    void contamComponentesEVerticesEmCiclos() {
        // CFCs: {A, B, C} e {D, E} com ciclo; {F} trivial
        Digrafo grafo = ListaDeArestas.ler("A -> B\nB -> C\nC -> A\nC -> D\nD -> E\nE -> D\nE -> F");
        List<List<Integer>> componentes = Malgrange.executar(grafo, false).componentes();

        Metricas metricas = Metricas.calcular(grafo, componentes, 1.5);

        assertEquals(6, metricas.vertices());
        assertEquals(7, metricas.arestas());
        assertEquals(3, metricas.componentes());
        assertEquals(2, metricas.componentesComCiclo());
        assertEquals(3, metricas.maiorComponente());
        assertEquals(5, metricas.verticesEmCiclos());
        assertEquals(100.0 * 5 / 6, metricas.percentualEmCiclos(), 1e-9);
        assertEquals(1.5, metricas.tempoMilissegundos());
    }

    @Test
    void grafoVazioNaoDividePorZero() {
        Metricas metricas = Metricas.calcular(new Digrafo(), List.of(), 0);

        assertEquals(0, metricas.componentes());
        assertEquals(0.0, metricas.percentualEmCiclos());
    }
}
