package br.unisagrado.ciclos.extracao;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Executa o {@code jdeps}, ferramenta que acompanha o JDK, para listar as dependências entre
 * classes de um .jar/.war (ou de uma pasta de .class).
 *
 * <p>O jdeps é usado SOMENTE para extrair os dados a partir do bytecode. Nenhum cálculo sobre
 * o grafo (componentes, ciclos etc.) é feito por ele.
 *
 * <p>Ele roda dentro da própria JVM via {@link ToolProvider}, então não depende do PATH, mas
 * exige que a aplicação seja executada com um JDK (um JRE não traz o jdeps).
 */
public final class ExecutorJdeps {

    /**
     * Pastas, dentro do arquivo, onde ficam as classes da aplicação quando ele empacota também
     * outras coisas: jar executável do Spring Boot e arquivo .war.
     */
    private static final List<String> PASTAS_DE_CLASSES_DA_APLICACAO = List.of("BOOT-INF/classes/", "WEB-INF/classes/");

    private ExecutorJdeps() {
    }

    /** Devolve a saída textual de {@code jdeps -verbose:class -filter:none} para o .jar/.war ou pasta. */
    public static String executar(Path jarOuPasta) {
        if (Files.isDirectory(jarOuPasta)) {
            return executarJdeps(jarOuPasta);
        }
        // Um jar do Spring Boot traz, além do projeto, as classes do lançador do Spring Boot na
        // raiz (org.springframework.boot.loader) e as bibliotecas em BOOT-INF/lib. Só interessa o
        // que está em BOOT-INF/classes (ou WEB-INF/classes num .war).
        String pastaDeClasses = pastaDeClassesDaAplicacao(jarOuPasta);
        // Um .jar com module-info faz o jdeps exigir todos os módulos declarados como dependência
        // (ex.: o Tomcat exige jakarta.annotation); extraindo os .class para uma pasta, ele
        // analisa o bytecode sem resolver módulos.
        if (pastaDeClasses.isEmpty() && !ehModular(jarOuPasta)) {
            return executarJdeps(jarOuPasta);
        }
        Path pastaTemporaria = null;
        try {
            pastaTemporaria = Files.createTempDirectory("jdeps-");
            extrairClasses(jarOuPasta, pastaDeClasses, pastaTemporaria);
            return executarJdeps(pastaTemporaria);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o .jar: " + e.getMessage(), e);
        } finally {
            apagarPasta(pastaTemporaria);
        }
    }

    private static String executarJdeps(Path alvo) {
        ToolProvider jdeps = ToolProvider.findFirst("jdeps").orElseThrow(() -> new IllegalStateException(
                "jdeps não está disponível. Execute a aplicação com um JDK (não um JRE)."));

        StringWriter saida = new StringWriter();
        StringWriter erros = new StringWriter();
        int codigoDeSaida = jdeps.run(new PrintWriter(saida, true), new PrintWriter(erros, true),
                // jars multi-release: analisa apenas as classes da versão base
                "--multi-release", "base",
                // uma linha por dependência entre classes
                "-verbose:class",
                // sem filtro: inclui dependências entre classes do mesmo pacote
                "-filter:none",
                alvo.toAbsolutePath().toString());

        if (codigoDeSaida != 0) {
            String mensagem = (erros + " " + saida).trim();
            throw new IllegalArgumentException("Falha ao executar o jdeps: " + mensagem);
        }
        return saida.toString();
    }

    /** O jar declara um módulo (module-info.class na raiz)? */
    private static boolean ehModular(Path jar) {
        try (JarFile arquivo = new JarFile(jar.toFile())) {
            return arquivo.getEntry("module-info.class") != null;
        } catch (IOException e) {
            throw jarInvalido(e);
        }
    }

    /** "BOOT-INF/classes/" ou "WEB-INF/classes/" se o arquivo tiver essa pasta; senão, "" (a raiz). */
    private static String pastaDeClassesDaAplicacao(Path jar) {
        try (JarFile arquivo = new JarFile(jar.toFile())) {
            for (String pasta : PASTAS_DE_CLASSES_DA_APLICACAO) {
                if (arquivo.stream().anyMatch(entrada -> entrada.getName().startsWith(pasta))) {
                    return pasta;
                }
            }
            return "";
        } catch (IOException e) {
            throw jarInvalido(e);
        }
    }

    private static IllegalArgumentException jarInvalido(IOException e) {
        return new IllegalArgumentException("Arquivo .jar/.war inválido: " + e.getMessage(), e);
    }

    /**
     * Copia para a pasta de destino os .class que estão em {@code pastaDeClasses} dentro do jar
     * ("" = raiz), ignorando module-info e as versões alternativas de jars multi-release
     * (META-INF/versions); só a versão base das classes é analisada.
     */
    private static void extrairClasses(Path jar, String pastaDeClasses, Path destino) throws IOException {
        try (InputStream arquivo = Files.newInputStream(jar); ZipInputStream zip = new ZipInputStream(arquivo)) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                if (!entrada.getName().startsWith(pastaDeClasses)) {
                    continue; // ex.: lançador do Spring Boot e bibliotecas em BOOT-INF/lib
                }
                String nome = entrada.getName().substring(pastaDeClasses.length());
                boolean ehClasse = !entrada.isDirectory() && nome.endsWith(".class");
                if (!ehClasse || nome.startsWith("META-INF/") || nome.endsWith("module-info.class")) {
                    continue;
                }
                Path caminho = destino.resolve(nome).normalize();
                if (!caminho.startsWith(destino)) {
                    continue; // entrada maliciosa tentando sair da pasta (zip slip)
                }
                Files.createDirectories(caminho.getParent());
                Files.copy(zip, caminho);
            }
        }
    }

    private static void apagarPasta(Path pasta) {
        if (pasta == null) {
            return;
        }
        try (Stream<Path> caminhos = Files.walk(pasta)) {
            caminhos.sorted(Comparator.reverseOrder()).forEach(caminho -> caminho.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
