package br.unisagrado.ciclos.extracao;

import br.unisagrado.ciclos.extracao.DependenciasDeClasses.Dependencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorJdepsTest {

    /** Classes compiladas do próprio projeto, usadas como material de teste. */
    private static final Path CLASSES_COMPILADAS = Path.of("target/classes/br/unisagrado/ciclos");

    @Test
    void analisaUmaPastaDeClassesCompiladas() {
        DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler(ExecutorJdeps.executar(Path.of("target/classes")));

        assertTrue(dependencias.classes().contains("br.unisagrado.ciclos.algoritmo.Malgrange"));
        assertTrue(dependencias.dependencias().contains(
                new Dependencia("br.unisagrado.ciclos.algoritmo.Malgrange", "br.unisagrado.ciclos.grafo.Digrafo")));
    }

    /**
     * Jar executável do Spring Boot: o projeto fica em BOOT-INF/classes e o lançador do Spring
     * Boot na raiz. Só as classes do projeto podem entrar na análise.
     */
    @Test
    void jarDoSpringBootAnalisaSoAsClassesDoProjeto(@TempDir Path pasta) throws IOException {
        Path jar = pasta.resolve("app.jar");
        try (JarOutputStream saida = new JarOutputStream(Files.newOutputStream(jar))) {
            adicionar(saida, "BOOT-INF/classes/br/unisagrado/ciclos/algoritmo/Malgrange.class", "algoritmo/Malgrange.class");
            adicionar(saida, "BOOT-INF/classes/br/unisagrado/ciclos/grafo/Digrafo.class", "grafo/Digrafo.class");
            // faz o papel do lançador do Spring Boot, que fica na raiz do jar
            adicionar(saida, "br/unisagrado/ciclos/analise/Metricas.class", "analise/Metricas.class");
        }

        DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler(ExecutorJdeps.executar(jar));

        assertEquals(Set.of("br.unisagrado.ciclos.algoritmo.Malgrange", "br.unisagrado.ciclos.grafo.Digrafo"),
                dependencias.classes());
    }

    @Test
    void arquivoQueNaoEhJarGeraErroClaro(@TempDir Path pasta) throws IOException {
        Path falso = pasta.resolve("falso.jar");
        Files.writeString(falso, "isto não é um jar");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> ExecutorJdeps.executar(falso));
        assertTrue(erro.getMessage().contains("inválido"), erro.getMessage());
    }

    private static void adicionar(JarOutputStream saida, String nomeNoJar, String classeCompilada) throws IOException {
        saida.putNextEntry(new JarEntry(nomeNoJar));
        Files.copy(CLASSES_COMPILADAS.resolve(classeCompilada), saida);
        saida.closeEntry();
    }
}
