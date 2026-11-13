package br.unisagrado.ciclos.api;

import br.unisagrado.ciclos.api.CatalogoDeInstancias.Instancia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoDeInstanciasTest {

    @Test
    void listaSoJarsWarsEListasDeArestas(@TempDir Path pasta) throws IOException {
        Files.writeString(pasta.resolve("b-grafo.txt"), "a -> b");
        Files.writeString(pasta.resolve("a-projeto.jar"), "x");
        Files.writeString(pasta.resolve("c-site.war"), "x");
        Files.writeString(pasta.resolve("leia-me.md"), "ignorado");
        Files.createDirectory(pasta.resolve("subpasta.txt"));

        List<Instancia> instancias = new CatalogoDeInstancias(pasta).listar();

        assertEquals(List.of("a-projeto.jar", "b-grafo.txt", "c-site.war"),
                instancias.stream().map(Instancia::nome).toList());
        assertEquals(List.of("jar", "arestas", "jar"), instancias.stream().map(Instancia::tipo).toList());
        assertEquals(6, instancias.get(1).tamanhoEmBytes());
    }

    @Test
    void resolveSoNomesDaListagem(@TempDir Path pasta) throws IOException {
        Files.writeString(pasta.resolve("grafo.txt"), "a -> b");
        CatalogoDeInstancias catalogo = new CatalogoDeInstancias(pasta);

        assertEquals(pasta.resolve("grafo.txt"), catalogo.resolver("grafo.txt"));
        assertThrows(NoSuchElementException.class, () -> catalogo.resolver("../segredo.txt"));
        assertThrows(NoSuchElementException.class, () -> catalogo.resolver("inexistente.txt"));
    }

    @Test
    void pastaInexistenteNaoTemInstancias(@TempDir Path pasta) {
        assertTrue(new CatalogoDeInstancias(pasta.resolve("nao-existe")).listar().isEmpty());
    }
}
