package br.unisagrado.ciclos.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioDeProjetosTest {

    private final RepositorioDeProjetos repositorio = new RepositorioDeProjetos();

    @Test
    void codigoCompiladoGeraGrafosDePacotesEDeClasses() {
        Projeto projeto = repositorio.carregarJar(Path.of("target/classes"), "próprio projeto");

        assertEquals(List.of(Projeto.NIVEL_PACOTES, Projeto.NIVEL_CLASSES), List.copyOf(projeto.grafos().keySet()));
        assertTrue(projeto.grafoDeClasses().indiceDe("br.unisagrado.ciclos.algoritmo.Malgrange") >= 0);
        assertEquals("jar", projeto.origem());
    }

    @Test
    void listaDeArestasGeraUmUnicoNivel() {
        Projeto projeto = repositorio.carregarListaDeArestas("a -> b", "manual");

        assertEquals(List.of(Projeto.NIVEL_INFORMADO), List.copyOf(projeto.grafos().keySet()));
        assertEquals(Projeto.NIVEL_INFORMADO, projeto.resolverNivel(null), "o primeiro nível é o padrão");
    }

    @Test
    void cadaProjetoRecebeUmIdEPodeSerBuscado() {
        Projeto primeiro = repositorio.carregarListaDeArestas("a -> b", "1");
        Projeto segundo = repositorio.carregarListaDeArestas("c -> d", "2");

        assertNotEquals(primeiro.id(), segundo.id());
        assertSame(segundo, repositorio.buscar(segundo.id()));
        assertThrows(NoSuchElementException.class, () -> repositorio.buscar("g999"));
    }

    @Test
    void pastaSemClassesEhRecusada(@TempDir Path pastaVazia) {
        assertThrows(IllegalArgumentException.class, () -> repositorio.carregarJar(pastaVazia, "vazia"));
    }

    @Test
    void ultimaAnaliseReaproveitaOResultado() {
        Projeto projeto = repositorio.carregarListaDeArestas("a -> b\nb -> a", "ciclo");

        Projeto.Analise analise = projeto.analisar(null, false);

        assertSame(analise, projeto.ultimaAnalise(null));
    }
}
