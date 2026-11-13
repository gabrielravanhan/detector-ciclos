package br.unisagrado.ciclos.analise;

import br.unisagrado.ciclos.algoritmo.Malgrange;
import br.unisagrado.ciclos.grafo.Digrafo;
import br.unisagrado.ciclos.grafo.ListaDeArestas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuscadorDeCiclosTest {

    @Test
    void encontraOMenorCicloDentroDaComponente() {
        Digrafo grafo = ListaDeArestas.ler("A -> B\nB -> C\nC -> A\nB -> D\nD -> E\nE -> D");
        List<Integer> componenteDeA = Malgrange.executar(grafo, false).componentes().get(0);

        List<Integer> ciclo = BuscadorDeCiclos.menorCicloPassandoPor(grafo, grafo.indiceDe("A"), componenteDeA);

        assertEquals(List.of("A", "B", "C", "A"), ciclo.stream().map(grafo::nome).toList());
    }

    @Test
    void preferenciaPeloCicloMaisCurto() {
        // dois ciclos passando por a: a → b → a (2 arestas) e a → c → d → a (3 arestas)
        Digrafo grafo = ListaDeArestas.ler("a -> c\nc -> d\nd -> a\na -> b\nb -> a");
        List<Integer> todos = List.of(0, 1, 2, 3);

        List<Integer> ciclo = BuscadorDeCiclos.menorCicloPassandoPor(grafo, 0, todos);

        assertEquals(List.of("a", "b", "a"), ciclo.stream().map(grafo::nome).toList());
    }

    @Test
    void naoSaiDaComponentePermitida() {
        // o único caminho de volta para a passa por x, que está fora dos permitidos
        Digrafo grafo = ListaDeArestas.ler("a -> x\nx -> a");

        assertTrue(BuscadorDeCiclos.menorCicloPassandoPor(grafo, 0, List.of(0)).isEmpty());
    }

    @Test
    void componenteTrivialNaoTemCiclo() {
        Digrafo grafo = ListaDeArestas.ler("x -> y");

        assertTrue(BuscadorDeCiclos.menorCicloPassandoPor(grafo, 0, List.of(0)).isEmpty());
    }
}
