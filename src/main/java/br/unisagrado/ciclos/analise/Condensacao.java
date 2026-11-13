package br.unisagrado.ciclos.analise;

import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grafo condensado: cada CFC vira um único vértice. O resultado é sempre acíclico (DAG) e mostra
 * a "arquitetura em camadas" real do projeto.
 *
 * @param componenteDe     componenteDe[v] = CFC do vértice v do grafo original
 * @param arestas          arestas entre CFCs; o peso soma os pesos das arestas originais
 * @param ordemTopologica  CFCs em ordem topológica (se A → B, A vem antes de B). Como A → B
 *                         significa "A depende de B", a ordem de compilação é a inversa desta.
 * @param camada           camada[c] = comprimento do maior caminho que chega em c; usado para
 *                         desenhar o grafo condensado em camadas
 */
public record Condensacao(
        int[] componenteDe,
        List<Aresta> arestas,
        List<Integer> ordemTopologica,
        int[] camada
) {

    public record Aresta(
            int origem,
            int destino,
            int peso
    ) {
    }

    public static Condensacao construir(Digrafo grafo, List<List<Integer>> componentes) {
        int quantidadeComponentes = componentes.size();
        int[] componenteDe = new int[grafo.quantidadeVertices()];
        for (int c = 0; c < quantidadeComponentes; c++) {
            for (int vertice : componentes.get(c)) {
                componenteDe[vertice] = c;
            }
        }

        // agrega as arestas que ligam CFCs diferentes
        List<Map<Integer, Integer>> adjacencia = new ArrayList<>();
        for (int c = 0; c < quantidadeComponentes; c++) {
            adjacencia.add(new LinkedHashMap<>());
        }
        for (int origem = 0; origem < grafo.quantidadeVertices(); origem++) {
            for (int destino : grafo.sucessores(origem)) {
                int componenteOrigem = componenteDe[origem];
                int componenteDestino = componenteDe[destino];
                if (componenteOrigem != componenteDestino) {
                    adjacencia.get(componenteOrigem).merge(componenteDestino, grafo.peso(origem, destino), Integer::sum);
                }
            }
        }

        List<Aresta> arestas = new ArrayList<>();
        int[] grauDeEntrada = new int[quantidadeComponentes];
        for (int c = 0; c < quantidadeComponentes; c++) {
            for (Map.Entry<Integer, Integer> saida : adjacencia.get(c).entrySet()) {
                arestas.add(new Aresta(c, saida.getKey(), saida.getValue()));
                grauDeEntrada[saida.getKey()]++;
            }
        }

        // ordenação topológica (algoritmo de Kahn) e camadas
        Deque<Integer> prontas = new ArrayDeque<>();
        for (int c = 0; c < quantidadeComponentes; c++) {
            if (grauDeEntrada[c] == 0) {
                prontas.add(c);
            }
        }
        List<Integer> ordem = new ArrayList<>();
        int[] camada = new int[quantidadeComponentes];
        while (!prontas.isEmpty()) {
            int c = prontas.poll();
            ordem.add(c);
            for (int proxima : adjacencia.get(c).keySet()) {
                camada[proxima] = Math.max(camada[proxima], camada[c] + 1);
                if (--grauDeEntrada[proxima] == 0) {
                    prontas.add(proxima);
                }
            }
        }

        // se as CFCs estiverem corretas, o grafo condensado nunca tem ciclo
        if (ordem.size() != quantidadeComponentes) {
            throw new IllegalStateException("O grafo condensado tem ciclo: as componentes informadas não são CFCs.");
        }
        return new Condensacao(componenteDe, arestas, ordem, camada);
    }
}
