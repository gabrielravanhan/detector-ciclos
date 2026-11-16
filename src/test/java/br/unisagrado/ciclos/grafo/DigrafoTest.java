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
    void arestaRepetidaEhGuardadaUmaVezSo() {
        Digrafo grafo = new Digrafo();
        grafo.adicionarAresta("A", "B");
        grafo.adicionarAresta("A", "B");
        grafo.adicionarAresta(grafo.indiceDe("A"), grafo.indiceDe("B"));

        assertEquals(1, grafo.quantidadeArestas());
        assertEquals(Set.of(grafo.indiceDe("B")), grafo.sucessores(grafo.indiceDe("A")));
        assertEquals(Set.of(grafo.indiceDe("A")), grafo.antecessores(grafo.indiceDe("B")));
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
    void rejeitaVerticeInexistente() {
        Digrafo grafo = new Digrafo();
        grafo.adicionarVertice("A");
        grafo.adicionarVertice("B");

        assertThrows(IndexOutOfBoundsException.class, () -> grafo.adicionarAresta(0, 7));
        assertThrows(IndexOutOfBoundsException.class, () -> grafo.nome(-1));
    }
}
