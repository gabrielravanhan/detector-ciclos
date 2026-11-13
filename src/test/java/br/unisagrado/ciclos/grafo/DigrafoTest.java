package br.unisagrado.ciclos.grafo;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigrafoTest {

    @Test
    void verticesRecebemIndicesNaOrdemDeInsercao() {
        Digrafo grafo = new Digrafo();

        assertEquals(0, grafo.adicionarVertice("A"));
        assertEquals(1, grafo.adicionarVertice("B"));
        assertEquals(0, grafo.adicionarVertice("A"), "adicionar de novo devolve o mesmo índice");

        assertEquals(2, grafo.quantidadeVertices());
        assertEquals("B", grafo.nome(1));
        assertEquals(1, grafo.indiceDe("B"));
        assertEquals(-1, grafo.indiceDe("inexistente"));
    }

    @Test
    void arestaFicaRegistradaNosDoisSentidos() {
        Digrafo grafo = new Digrafo();
        grafo.adicionarAresta("A", "B");
        grafo.adicionarAresta("A", "C");

        int a = grafo.indiceDe("A");
        int b = grafo.indiceDe("B");
        int c = grafo.indiceDe("C");

        assertEquals(Set.of(b, c), grafo.sucessores(a));
        assertEquals(Set.of(a), grafo.antecessores(b));
        assertTrue(grafo.sucessores(b).isEmpty(), "a aresta é direcionada: B não aponta para A");
        assertEquals(2, grafo.quantidadeArestas());
    }

    @Test
    void arestaRepetidaSomaPesoEmVezDeDuplicar() {
        Digrafo grafo = new Digrafo();
        grafo.adicionarAresta("A", "B");
        grafo.adicionarAresta("A", "B");
        grafo.adicionarAresta(grafo.indiceDe("A"), grafo.indiceDe("B"), 3);

        assertEquals(1, grafo.quantidadeArestas());
        assertEquals(5, grafo.peso(grafo.indiceDe("A"), grafo.indiceDe("B")));
        assertEquals(0, grafo.peso(grafo.indiceDe("B"), grafo.indiceDe("A")), "aresta inexistente tem peso 0");
    }

    @Test
    void lacoEhIgnorado() {
        Digrafo grafo = new Digrafo();
        grafo.adicionarAresta("A", "A");

        assertEquals(1, grafo.quantidadeVertices());
        assertEquals(0, grafo.quantidadeArestas());
        assertTrue(grafo.sucessores(0).isEmpty());
    }

    @Test
    void rejeitaPesoInvalidoEVerticeInexistente() {
        Digrafo grafo = new Digrafo();
        grafo.adicionarVertice("A");
        grafo.adicionarVertice("B");

        assertThrows(IllegalArgumentException.class, () -> grafo.adicionarAresta(0, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> grafo.adicionarAresta(0, 7, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> grafo.nome(-1));
    }
}
