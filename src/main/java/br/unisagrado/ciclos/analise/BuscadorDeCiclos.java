package br.unisagrado.ciclos.analise;

import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encontra um ciclo concreto dentro de uma CFC, para mostrar ao desenvolvedor uma evidência do
 * problema (ex.: web → service → repository → web).
 */
public final class BuscadorDeCiclos {

    private BuscadorDeCiclos() {
    }

    /**
     * Menor ciclo que passa por {@code inicio} usando só vértices de {@code permitidos}.
     * Faz uma busca em largura a partir de inicio até achar um vértice com aresta de volta para
     * inicio; por ser busca em largura, o ciclo encontrado tem o menor número de arestas.
     *
     * @return [inicio, ..., inicio], ou lista vazia se inicio não está em nenhum ciclo
     */
    public static List<Integer> menorCicloPassandoPor(Digrafo grafo, int inicio, Collection<Integer> permitidos) {
        Set<Integer> conjuntoPermitido = new HashSet<>(permitidos);
        Map<Integer, Integer> anterior = new HashMap<>();
        Deque<Integer> fila = new ArrayDeque<>();
        anterior.put(inicio, inicio);
        fila.add(inicio);

        while (!fila.isEmpty()) {
            int vertice = fila.poll();
            for (int proximo : grafo.sucessores(vertice)) {
                if (proximo == inicio) {
                    return caminhoAte(vertice, inicio, anterior);
                }
                if (conjuntoPermitido.contains(proximo) && !anterior.containsKey(proximo)) {
                    anterior.put(proximo, vertice);
                    fila.add(proximo);
                }
            }
        }
        return List.of();
    }

    /** Reconstrói o caminho inicio → ... → ultimo seguindo os anteriores e fecha o ciclo em inicio. */
    private static List<Integer> caminhoAte(int ultimo, int inicio, Map<Integer, Integer> anterior) {
        List<Integer> caminho = new ArrayList<>();
        caminho.add(inicio);
        for (int vertice = ultimo; vertice != inicio; vertice = anterior.get(vertice)) {
            caminho.add(vertice);
        }
        caminho.add(inicio);
        Collections.reverse(caminho);
        return caminho;
    }
}
