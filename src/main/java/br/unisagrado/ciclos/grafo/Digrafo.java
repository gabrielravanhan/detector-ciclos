package br.unisagrado.ciclos.grafo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo direcionado e ponderado, implementado pelo grupo (nenhuma biblioteca de grafos é usada).
 *
 * <p>Cada vértice tem um nome (ex.: nome de um pacote ou de uma classe) e um índice inteiro
 * de 0 a n-1, atribuído na ordem de inserção. Os algoritmos trabalham com os índices; os nomes
 * aparecem apenas na entrada e na saída.
 *
 * <p>Representação: listas de adjacência nos dois sentidos. Guardar também os antecessores
 * permite ao Malgrange calcular o fecho inverso Γ⁻ sem precisar inverter o grafo.
 *
 * <p>O peso de uma aresta u → v conta quantas vezes a dependência foi registrada
 * (ex.: quantos pares de classes ligam dois pacotes). Laços (u → u) são ignorados: um elemento
 * que depende de si mesmo não forma ciclo entre elementos distintos.
 */
public final class Digrafo {

    private final List<String> nomes = new ArrayList<>();
    private final Map<String, Integer> indicePorNome = new HashMap<>();
    /** sucessores.get(u) mapeia cada sucessor v de u ao peso da aresta u → v. */
    private final List<Map<Integer, Integer>> sucessores = new ArrayList<>();
    private final List<Set<Integer>> antecessores = new ArrayList<>();
    private int quantidadeArestas;

    /** Adiciona o vértice, se ainda não existir, e devolve seu índice. */
    public int adicionarVertice(String nome) {
        Integer existente = indicePorNome.get(nome);
        if (existente != null) {
            return existente;
        }
        int indice = nomes.size();
        nomes.add(nome);
        indicePorNome.put(nome, indice);
        sucessores.add(new LinkedHashMap<>());
        antecessores.add(new LinkedHashSet<>());
        return indice;
    }

    /** Adiciona a aresta origem → destino com peso 1, criando os vértices se necessário. */
    public void adicionarAresta(String origem, String destino) {
        adicionarAresta(adicionarVertice(origem), adicionarVertice(destino), 1);
    }

    /** Adiciona a aresta origem → destino. Se ela já existir, o peso é somado ao anterior. */
    public void adicionarAresta(int origem, int destino, int peso) {
        validarVertice(origem);
        validarVertice(destino);
        if (peso <= 0) {
            throw new IllegalArgumentException("O peso da aresta deve ser positivo: " + peso);
        }
        if (origem == destino) {
            return;
        }
        Map<Integer, Integer> saidas = sucessores.get(origem);
        if (!saidas.containsKey(destino)) {
            quantidadeArestas++;
            antecessores.get(destino).add(origem);
        }
        saidas.merge(destino, peso, Integer::sum);
    }

    public int quantidadeVertices() {
        return nomes.size();
    }

    public int quantidadeArestas() {
        return quantidadeArestas;
    }

    public String nome(int vertice) {
        validarVertice(vertice);
        return nomes.get(vertice);
    }

    /** Índice do vértice com esse nome, ou -1 se não existir. */
    public int indiceDe(String nome) {
        return indicePorNome.getOrDefault(nome, -1);
    }

    /** Γ(v): sucessores diretos de v. */
    public Set<Integer> sucessores(int vertice) {
        validarVertice(vertice);
        return Collections.unmodifiableSet(sucessores.get(vertice).keySet());
    }

    /** Γ⁻¹(v): antecessores diretos de v. */
    public Set<Integer> antecessores(int vertice) {
        validarVertice(vertice);
        return Collections.unmodifiableSet(antecessores.get(vertice));
    }

    /** Peso da aresta origem → destino, ou 0 se ela não existir. */
    public int peso(int origem, int destino) {
        validarVertice(origem);
        validarVertice(destino);
        return sucessores.get(origem).getOrDefault(destino, 0);
    }

    private void validarVertice(int vertice) {
        if (vertice < 0 || vertice >= nomes.size()) {
            throw new IndexOutOfBoundsException("Vértice inexistente: " + vertice);
        }
    }
}
