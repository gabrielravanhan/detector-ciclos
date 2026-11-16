package br.unisagrado.ciclos.grafo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato textual simples de grafo, usado para montar instâncias à mão (inclusive durante a
 * apresentação).
 *
 * <pre>
 * # comentário
 * A -&gt; B          aresta de A para B
 * D               vértice isolado
 * </pre>
 * A ordem dos vértices segue a primeira aparição no texto; é nessa ordem que o Malgrange
 * escolhe os pivôs, o que facilita conferir o resultado com um cálculo feito à mão.
 */
public final class ListaDeArestas {

    private static final Pattern ARESTA = Pattern.compile("^(\\S+)\\s*->\\s*(\\S+)$");
    private static final Pattern VERTICE = Pattern.compile("^(\\S+)$");

    private ListaDeArestas() {
    }

    /** Monta o grafo descrito no texto. Linhas em branco e comentários (#) são ignorados. */
    public static Digrafo ler(String texto) {
        Digrafo grafo = new Digrafo();
        String[] linhas = texto.split("\\R");
        for (int i = 0; i < linhas.length; i++) {
            String linha = removerComentario(linhas[i]).trim();
            if (linha.isEmpty()) {
                continue;
            }
            Matcher aresta = ARESTA.matcher(linha);
            Matcher vertice = VERTICE.matcher(linha);
            if (aresta.matches()) {
                grafo.adicionarAresta(aresta.group(1), aresta.group(2));
            } else if (vertice.matches()) {
                grafo.adicionarVertice(vertice.group(1));
            } else {
                throw new IllegalArgumentException("Linha " + (i + 1) + " inválida: \"" + linhas[i].trim()
                        + "\". Use \"A -> B\" para uma aresta ou só \"A\" para um vértice isolado.");
            }
        }
        return grafo;
    }

    private static String removerComentario(String linha) {
        int cerquilha = linha.indexOf('#');
        return cerquilha < 0 ? linha : linha.substring(0, cerquilha);
    }
}
