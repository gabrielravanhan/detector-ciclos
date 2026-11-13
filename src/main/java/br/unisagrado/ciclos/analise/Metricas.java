package br.unisagrado.ciclos.analise;

import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.List;

/**
 * Indicadores da estrutura de dependências.
 *
 * @param componentesComCiclo CFCs com mais de um vértice, ou seja, ciclos de dependência
 * @param verticesEmCiclos    vértices que pertencem a alguma CFC com ciclo
 * @param tempoMilissegundos  tempo de execução do Malgrange
 */
public record Metricas(
        int vertices,
        int arestas,
        int componentes,
        int componentesComCiclo,
        int maiorComponente,
        int verticesEmCiclos,
        double percentualEmCiclos,
        double tempoMilissegundos
) {

    public static Metricas calcular(Digrafo grafo, List<List<Integer>> componentes, double tempoMilissegundos) {
        int comCiclo = 0;
        int maior = 0;
        int emCiclos = 0;
        for (List<Integer> componente : componentes) {
            maior = Math.max(maior, componente.size());
            if (componente.size() > 1) {
                comCiclo++;
                emCiclos += componente.size();
            }
        }
        double percentual = grafo.quantidadeVertices() == 0 ? 0 : 100.0 * emCiclos / grafo.quantidadeVertices();
        return new Metricas(grafo.quantidadeVertices(), grafo.quantidadeArestas(), componentes.size(), comCiclo,
                maior, emCiclos, percentual, tempoMilissegundos);
    }
}
