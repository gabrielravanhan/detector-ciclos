package br.unisagrado.ciclos.grafo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo direcionado, implementado pelo grupo (nenhuma biblioteca de grafos é usada).
 *
 * <p>Cada vértice tem um nome (ex.: nome de um pacote ou de uma classe) e um índice inteiro
 * de 0 a n-1, atribuído na ordem de inserção. Os algoritmos trabalham com os índices; os nomes
 * aparecem apenas na entrada e na saída.
 *
 * <p>Representação: listas de adjacência nos dois sentidos. Guardar também os antecessores
 * permite ao Malgrange calcular o fecho inverso Γ⁻ sem precisar inverter o grafo.
 *
 * <p>Uma aresta repetida é guardada uma vez só. Laços (u → u) são ignorados: um elemento
 * que depende de si mesmo não forma ciclo entre elementos distintos.
 */
public final class Digrafo {

    private final List<String> nomes = new ArrayList<>();
    private final Map<String, Integer> indicePorNome = new HashMap<>();
    private final List<Set<Integer>> sucessores = new ArrayList<>();
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
        sucessores.add(new LinkedHashSet<>());
        antecessores.add(new LinkedHashSet<>());
        return indice;
    }

    /** Adiciona a aresta origem → destino, criando os vértices se necessário. */
    public void adicionarAresta(String origem, String destino) {
        adicionarAresta(adicionarVertice(origem), adicionarVertice(destino));
    }

    /** Adiciona a aresta origem → destino. Se ela já existir, nada muda. */
    public void adicionarAresta(int origem, int destino) {
        validarVertice(origem);
        validarVertice(destino);
        if (origem == destino) {
            return;
        }
        if (sucessores.get(origem).add(destino)) {
            antecessores.get(destino).add(origem);
            quantidadeArestas++;
        }
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
        return Collections.unmodifiableSet(sucessores.get(vertice));
    }

    /** Γ⁻¹(v): antecessores diretos de v. */
    public Set<Integer> antecessores(int vertice) {
        validarVertice(vertice);
        return Collections.unmodifiableSet(antecessores.get(vertice));
    }

    private void validarVertice(int vertice) {
        if (vertice < 0 || vertice >= nomes.size()) {
            throw new IndexOutOfBoundsException("Vértice inexistente: " + vertice);
        }
    }
}
