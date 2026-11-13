package br.unisagrado.ciclos.extracao;

import br.unisagrado.ciclos.extracao.DependenciasDeClasses.Dependencia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta a saída de {@code jdeps -verbose:class}.
 *
 * <p>As linhas de interesse têm o formato (recuadas com espaços):
 * <pre>
 *    com.google.gson.Gson   -&gt; com.google.gson.internal.Excluder   gson-2.10.1.jar
 *    com.google.gson.Gson   -&gt; java.lang.String                    java.base
 * </pre>
 * Linhas sem recuo são resumos por pacote e são ignoradas.
 *
 * <p>Decisões de modelagem:
 * <ul>
 *   <li>classes internas e anônimas ({@code Gson$1}, {@code Gson$Builder}) são agrupadas na
 *       classe de nível superior ({@code Gson}), pois ficam no mesmo arquivo-fonte;</li>
 *   <li>só ficam dependências cujo destino também é uma classe do projeto analisado
 *       (descarta {@code java.*} e bibliotecas de terceiros);</li>
 *   <li>{@code package-info} e {@code module-info} são ignorados.</li>
 * </ul>
 */
public final class LeitorSaidaJdeps {

    private static final Pattern LINHA_DE_DEPENDENCIA = Pattern.compile("^\\s+(\\S+)\\s+->\\s+(\\S+)(?:\\s+.*)?$");

    private LeitorSaidaJdeps() {
    }

    public static DependenciasDeClasses ler(String saidaJdeps) {
        List<Dependencia> brutas = new ArrayList<>();
        Set<String> classesBrutas = new HashSet<>();

        for (String linha : saidaJdeps.split("\\R")) {
            Matcher correspondencia = LINHA_DE_DEPENDENCIA.matcher(linha);
            if (!correspondencia.matches() || ehMetadado(correspondencia.group(1))) {
                continue;
            }
            // toda classe que aparece como origem pertence ao projeto analisado
            classesBrutas.add(correspondencia.group(1));
            brutas.add(new Dependencia(correspondencia.group(1), correspondencia.group(2)));
        }

        Set<String> classes = new TreeSet<>();
        List<Dependencia> internas = new ArrayList<>();
        for (Dependencia dependencia : brutas) {
            String origem = classeDeNivelSuperior(dependencia.origem(), classesBrutas);
            String destino = classeDeNivelSuperior(dependencia.destino(), classesBrutas);
            classes.add(origem);
            boolean destinoEhDoProjeto = classesBrutas.contains(destino);
            if (destinoEhDoProjeto && !origem.equals(destino)) {
                internas.add(new Dependencia(origem, destino));
            }
        }
        return new DependenciasDeClasses(classes, internas);
    }

    /**
     * Classe de nível superior que contém {@code nomeDaClasse}: {@code a.b.Foo$Bar$1} → {@code a.b.Foo}.
     *
     * <p>O '$' também pode fazer parte do nome de uma classe comum (ex.: {@code $Gson$Types}, do
     * Gson). Por isso só se corta num '$' quando o prefixo é de fato uma classe do projeto.
     */
    static String classeDeNivelSuperior(String nomeDaClasse, Set<String> classesConhecidas) {
        int inicioDoNomeSimples = nomeDaClasse.lastIndexOf('.') + 1;
        for (int i = nomeDaClasse.indexOf('$', inicioDoNomeSimples + 1); i > 0; i = nomeDaClasse.indexOf('$', i + 1)) {
            String prefixo = nomeDaClasse.substring(0, i);
            if (classesConhecidas.contains(prefixo)) {
                return prefixo;
            }
        }
        return nomeDaClasse;
    }

    private static boolean ehMetadado(String nomeDaClasse) {
        return nomeDaClasse.endsWith("package-info") || nomeDaClasse.endsWith("module-info");
    }
}
