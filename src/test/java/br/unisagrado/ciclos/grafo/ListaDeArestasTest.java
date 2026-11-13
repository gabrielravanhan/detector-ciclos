package br.unisagrado.ciclos.grafo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListaDeArestasTest {

    @Test
    void leArestasPesosEVerticesIsolados() {
        Digrafo grafo = ListaDeArestas.ler("""
                # exemplo
                A -> B
                B -> C 3

                D          # vértice isolado
                """);

        assertEquals(4, grafo.quantidadeVertices());
        assertEquals(2, grafo.quantidadeArestas());
        assertEquals(1, grafo.peso(grafo.indiceDe("A"), grafo.indiceDe("B")));
        assertEquals(3, grafo.peso(grafo.indiceDe("B"), grafo.indiceDe("C")));
        assertTrue(grafo.sucessores(grafo.indiceDe("D")).isEmpty());
    }

    @Test
    void ordemDosVerticesSegueAPrimeiraAparicao() {
        Digrafo grafo = ListaDeArestas.ler("C -> A\nB -> C");

        assertEquals("C", grafo.nome(0));
        assertEquals("A", grafo.nome(1));
        assertEquals("B", grafo.nome(2));
    }

    @Test
    void aceitaSetaSemEspacosENomesDePacote() {
        Digrafo grafo = ListaDeArestas.ler("com.loja.web->com.loja.service");

        assertEquals(1, grafo.quantidadeArestas());
        assertEquals(0, grafo.indiceDe("com.loja.web"));
    }

    @Test
    void textoVazioGeraGrafoVazio() {
        assertEquals(0, ListaDeArestas.ler("").quantidadeVertices());
        assertEquals(0, ListaDeArestas.ler("# só comentário\n\n").quantidadeVertices());
    }

    @Test
    void escreverELerReconstroemOMesmoTexto() {
        String texto = "a -> b\nb -> c 3\nd\n";

        assertEquals(texto, ListaDeArestas.escrever(ListaDeArestas.ler(texto)));
    }

    @Test
    void linhaInvalidaInformaONumeroDaLinha() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ListaDeArestas.ler("a -> b\na b c d"));

        assertTrue(erro.getMessage().contains("Linha 2"), erro.getMessage());
    }

    @Test
    void pesoZeroEhRejeitado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ListaDeArestas.ler("a -> b 0"));

        assertTrue(erro.getMessage().contains("peso"), erro.getMessage());
    }
}
