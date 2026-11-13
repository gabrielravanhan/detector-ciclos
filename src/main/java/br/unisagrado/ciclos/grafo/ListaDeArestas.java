package br.unisagrado.ciclos.grafo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato textual simples de grafo, usado para montar instâncias à mão (inclusive durante a
 * apresentação) e para exportar grafos para validação externa.
 *
 * <pre>
 * # comentário
 * A -&gt; B          aresta de peso 1
 * A -&gt; C 3        aresta de peso 3
 * D               vértice isolado
 * </pre>
 * A ordem dos vértices segue a primeira aparição no texto; é nessa ordem que o Malgrange
 * escolhe os pivôs, o que facilita conferir o resultado com um cálculo feito à mão.
 */
public final class ListaDeArestas {

    private static final Pattern ARESTA = Pattern.compile("^(\\S+)\\s*->\\s*(\\S+)(?:\\s+(\\d{1,9}))?$");
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
                int peso = aresta.group(3) == null ? 1 : Integer.parseInt(aresta.group(3));
                if (peso <= 0) {
                    throw new IllegalArgumentException("Linha " + (i + 1) + ": o peso deve ser positivo");
                }
                int origem = grafo.adicionarVertice(aresta.group(1));
                int destino = grafo.adicionarVertice(aresta.group(2));
                grafo.adicionarAresta(origem, destino, peso);
            } else if (vertice.matches()) {
                grafo.adicionarVertice(vertice.group(1));
            } else {
                throw new IllegalArgumentException("Linha " + (i + 1) + " inválida: \"" + linhas[i].trim()
                        + "\". Use \"A -> B\" ou \"A -> B peso\".");
            }
        }
        return grafo;
    }

    /** Gera o texto do grafo; {@code ler(escrever(g))} reconstrói o mesmo grafo. */
    public static String escrever(Digrafo grafo) {
        StringBuilder texto = new StringBuilder();
        for (int v = 0; v < grafo.quantidadeVertices(); v++) {
            boolean isolado = grafo.sucessores(v).isEmpty() && grafo.antecessores(v).isEmpty();
            if (isolado) {
                texto.append(grafo.nome(v)).append('\n');
            }
            for (int w : grafo.sucessores(v)) {
                texto.append(grafo.nome(v)).append(" -> ").append(grafo.nome(w));
                int peso = grafo.peso(v, w);
                if (peso != 1) {
                    texto.append(' ').append(peso);
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    private static String removerComentario(String linha) {
        int cerquilha = linha.indexOf('#');
        return cerquilha < 0 ? linha : linha.substring(0, cerquilha);
    }
}
