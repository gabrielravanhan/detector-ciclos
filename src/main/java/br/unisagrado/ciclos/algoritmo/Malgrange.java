package br.unisagrado.ciclos.algoritmo;

import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Algoritmo de Malgrange para encontrar as componentes fortemente conexas (CFC) de um grafo
 * direcionado.
 *
 * <p><b>Ideia:</b> dois vértices u e v estão na mesma CFC se e somente se u alcança v e v
 * alcança u. Escolhido um vértice pivô v:
 * <ul>
 *   <li>Γ⁺(v) — fecho transitivo direto: v e todos os vértices alcançáveis a partir de v;</li>
 *   <li>Γ⁻(v) — fecho transitivo inverso: v e todos os vértices que alcançam v;</li>
 *   <li>a CFC de v é Γ⁺(v) ∩ Γ⁻(v).</li>
 * </ul>
 * Os vértices da CFC encontrada são removidos e o processo se repete com o próximo vértice ainda
 * não classificado, até que todos pertençam a alguma CFC.
 *
 * <p><b>Fechos por níveis:</b> como na formulação clássica, Γ⁺ é construído nível a nível:
 * nível 0 = {v}, nível 1 = Γ(v), nível 2 = vértices novos em Γ²(v), ... até não surgir vértice
 * novo. O mesmo vale para Γ⁻ com os antecessores. Cada nível contém apenas os vértices alcançados
 * pela primeira vez naquele passo.
 *
 * <p><b>Restrição aos vértices restantes:</b> os fechos ignoram vértices já removidos. Isso não
 * altera o resultado: os removidos formam CFCs completas, e um caminho entre dois vértices da
 * mesma CFC nunca passa por um vértice de outra CFC.
 *
 * <p><b>Complexidade:</b> cada iteração calcula dois fechos em O(V + E) e há no máximo V
 * iterações, portanto O(V · (V + E)) no pior caso (contra O(V + E) de Tarjan ou Kosaraju).
 */
public final class Malgrange {

    private Malgrange() {
    }

    /**
     * @param registrarPassos se verdadeiro, guarda os níveis de Γ⁺ e Γ⁻ de cada iteração para
     *                        exibição passo a passo (consome memória O(V²) no pior caso)
     */
    public static ResultadoMalgrange executar(Digrafo grafo, boolean registrarPassos) {
        int n = grafo.quantidadeVertices();
        boolean[] removido = new boolean[n];
        List<List<Integer>> componentes = new ArrayList<>();
        List<PassoMalgrange> passos = new ArrayList<>();

        for (int pivo = 0; pivo < n; pivo++) {
            if (removido[pivo]) {
                continue; // já pertence a uma CFC encontrada antes
            }

            Fecho fechoDireto = fecho(grafo, pivo, removido, Direcao.SUCESSORES);    // Γ⁺(pivô)
            Fecho fechoInverso = fecho(grafo, pivo, removido, Direcao.ANTECESSORES); // Γ⁻(pivô)

            // CFC(pivô) = Γ⁺(pivô) ∩ Γ⁻(pivô)
            List<Integer> componente = new ArrayList<>();
            for (List<Integer> nivel : fechoInverso.niveis()) {
                for (int vertice : nivel) {
                    if (fechoDireto.contem(vertice)) {
                        componente.add(vertice);
                    }
                }
            }

            for (int vertice : componente) {
                removido[vertice] = true;
            }
            componentes.add(componente);
            if (registrarPassos) {
                passos.add(new PassoMalgrange(pivo, fechoDireto.niveis(), fechoInverso.niveis(), componente));
            }
        }
        return new ResultadoMalgrange(componentes, passos);
    }

    enum Direcao { SUCESSORES, ANTECESSORES }

    /**
     * Fecho transitivo de {@code inicio} separado por níveis (busca em largura que não visita
     * vértices removidos).
     */
    static Fecho fecho(Digrafo grafo, int inicio, boolean[] removido, Direcao direcao) {
        Set<Integer> alcancados = new HashSet<>();
        List<List<Integer>> niveis = new ArrayList<>();

        alcancados.add(inicio);
        List<Integer> nivelAtual = List.of(inicio);
        while (!nivelAtual.isEmpty()) {
            niveis.add(nivelAtual);
            List<Integer> proximoNivel = new ArrayList<>();
            for (int vertice : nivelAtual) {
                Set<Integer> vizinhos = direcao == Direcao.SUCESSORES
                        ? grafo.sucessores(vertice)
                        : grafo.antecessores(vertice);
                for (int vizinho : vizinhos) {
                    if (!removido[vizinho] && alcancados.add(vizinho)) {
                        proximoNivel.add(vizinho);
                    }
                }
            }
            nivelAtual = proximoNivel;
        }
        return new Fecho(niveis, alcancados);
    }

    /**
     * @param niveis  nível k = vértices alcançados pela primeira vez com k arestas
     * @param membros união de todos os níveis
     */
    record Fecho(List<List<Integer>> niveis, Set<Integer> membros) {
        boolean contem(int vertice) {
            return membros.contains(vertice);
        }
    }
}
