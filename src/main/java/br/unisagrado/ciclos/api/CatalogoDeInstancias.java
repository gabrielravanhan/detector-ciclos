package br.unisagrado.ciclos.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Instâncias prontas para a demonstração: arquivos .jar/.war e .txt (lista de arestas) de uma pasta.
 */
public final class CatalogoDeInstancias {

    public record Instancia(
            String nome,
            String tipo,
            long tamanhoEmBytes
    ) {
    }

    private final Path pasta;

    public CatalogoDeInstancias(Path pasta) {
        this.pasta = pasta;
    }

    public Path pasta() {
        return pasta;
    }

    /** Arquivo Java compilado (.jar ou .war), analisado com o jdeps. */
    public static boolean ehArquivoCompilado(String nomeDoArquivo) {
        return nomeDoArquivo.endsWith(".jar") || nomeDoArquivo.endsWith(".war");
    }

    public List<Instancia> listar() {
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        List<Instancia> instancias = new ArrayList<>();
        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.sorted(Comparator.comparing(Path::getFileName)).toList()) {
                String nome = arquivo.getFileName().toString();
                String tipo = ehArquivoCompilado(nome) ? "jar" : nome.endsWith(".txt") ? "arestas" : null;
                if (tipo != null && Files.isRegularFile(arquivo)) {
                    instancias.add(new Instancia(nome, tipo, Files.size(arquivo)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return instancias;
    }

    /** Só aceita nomes que estão na listagem (impede acessar arquivos fora da pasta). */
    public Path resolver(String nome) {
        for (Instancia instancia : listar()) {
            if (instancia.nome().equals(nome)) {
                return pasta.resolve(instancia.nome());
            }
        }
        throw new NoSuchElementException("Instância \"" + nome + "\" não encontrada em " + pasta);
    }
}
