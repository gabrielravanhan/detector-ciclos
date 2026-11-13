package br.unisagrado.ciclos.instancia;

import br.unisagrado.ciclos.common.exception.NaoEncontradoException;
import br.unisagrado.ciclos.instancia.dto.response.InstanciaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanciaServiceTest {

    @Test
    void listaSoJarsWarsEListasDeArestas(@TempDir Path pasta) throws IOException {
        Files.writeString(pasta.resolve("b-grafo.txt"), "a -> b");
        Files.writeString(pasta.resolve("a-projeto.jar"), "x");
        Files.writeString(pasta.resolve("c-site.war"), "x");
        Files.writeString(pasta.resolve("leia-me.md"), "ignorado");
        Files.createDirectory(pasta.resolve("subpasta.txt"));

        List<InstanciaResponse> instancias = new InstanciaService(pasta).listar();

        assertEquals(List.of("a-projeto.jar", "b-grafo.txt", "c-site.war"),
                instancias.stream().map(InstanciaResponse::nome).toList());
        assertEquals(List.of("jar", "arestas", "jar"), instancias.stream().map(InstanciaResponse::tipo).toList());
        assertEquals(6, instancias.get(1).tamanhoEmBytes());
    }

    @Test
    void resolveSoNomesDaListagem(@TempDir Path pasta) throws IOException {
        Files.writeString(pasta.resolve("grafo.txt"), "a -> b");
        InstanciaService instanciaService = new InstanciaService(pasta);

        assertEquals(pasta.resolve("grafo.txt"), instanciaService.resolver("grafo.txt"));
        assertThrows(NaoEncontradoException.class, () -> instanciaService.resolver("../segredo.txt"));
        assertThrows(NaoEncontradoException.class, () -> instanciaService.resolver("inexistente.txt"));
    }

    @Test
    void pastaInexistenteNaoTemInstancias(@TempDir Path pasta) {
        assertTrue(new InstanciaService(pasta.resolve("nao-existe")).listar().isEmpty());
    }
}
