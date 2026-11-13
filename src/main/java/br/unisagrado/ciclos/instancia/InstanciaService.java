package br.unisagrado.ciclos.instancia;

import br.unisagrado.ciclos.common.exception.NaoEncontradoException;
import br.unisagrado.ciclos.instancia.dto.response.InstanciaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Instâncias prontas para a demonstração: arquivos .jar/.war e .txt (lista de arestas) de uma pasta.
 * A pasta vem da propriedade "instancias" (application.yaml ou --instancias=...).
 */
@Service
public class InstanciaService {

    private final Path pasta;

    @Autowired
    public InstanciaService(@Value("${instancias}") String pasta) {
        this(Path.of(pasta));
    }

    public InstanciaService(Path pasta) {
        this.pasta = pasta;
    }

    public Path pasta() {
        return pasta;
    }

    /** Arquivo Java compilado (.jar ou .war), analisado com o jdeps. */
    public static boolean ehArquivoCompilado(String nomeDoArquivo) {
        return nomeDoArquivo.endsWith(".jar") || nomeDoArquivo.endsWith(".war");
    }

    public List<InstanciaResponse> listar() {
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        List<InstanciaResponse> instancias = new ArrayList<>();
        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.sorted(Comparator.comparing(Path::getFileName)).toList()) {
                String nome = arquivo.getFileName().toString();
                String tipo = ehArquivoCompilado(nome) ? "jar" : nome.endsWith(".txt") ? "arestas" : null;
                if (tipo != null && Files.isRegularFile(arquivo)) {
                    instancias.add(new InstanciaResponse(nome, tipo, Files.size(arquivo)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return instancias;
    }

    /** Só aceita nomes que estão na listagem (impede acessar arquivos fora da pasta). */
    public Path resolver(String nome) {
        for (InstanciaResponse instancia : listar()) {
            if (instancia.nome().equals(nome)) {
                return pasta.resolve(instancia.nome());
            }
        }
        throw new NaoEncontradoException("Instância \"" + nome + "\" não encontrada em " + pasta);
    }
}
