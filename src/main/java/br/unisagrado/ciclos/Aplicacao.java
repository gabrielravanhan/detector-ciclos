package br.unisagrado.ciclos;

import br.unisagrado.ciclos.api.CatalogoDeInstancias;
import br.unisagrado.ciclos.api.RepositorioDeProjetos;
import br.unisagrado.ciclos.api.ServidorApi;

import java.nio.file.Path;

/**
 * Ponto de entrada. Uso:
 * <pre>java -jar target/detector-ciclos.jar [--porta=7070] [--instancias=instancias]</pre>
 */
public final class Aplicacao {

    public static void main(String[] argumentos) {
        int porta = 7070;
        Path pastaDeInstancias = Path.of("instancias");
        for (String argumento : argumentos) {
            if (argumento.startsWith("--porta=")) {
                porta = Integer.parseInt(argumento.substring("--porta=".length()));
            } else if (argumento.startsWith("--instancias=")) {
                pastaDeInstancias = Path.of(argumento.substring("--instancias=".length()));
            } else {
                System.err.println("Argumento desconhecido: " + argumento);
                System.exit(1);
            }
        }

        CatalogoDeInstancias catalogo = new CatalogoDeInstancias(pastaDeInstancias);
        new ServidorApi(new RepositorioDeProjetos(), catalogo).criar().start(porta);

        System.out.println("Detector de ciclos rodando em http://localhost:" + porta);
        System.out.println("Instâncias: " + catalogo.pasta().toAbsolutePath() + " (" + catalogo.listar().size() + " arquivos)");
    }
}
