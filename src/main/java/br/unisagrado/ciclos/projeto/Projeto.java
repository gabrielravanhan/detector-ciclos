package br.unisagrado.ciclos.projeto;

import br.unisagrado.ciclos.algoritmo.Malgrange;
import br.unisagrado.ciclos.algoritmo.ResultadoMalgrange;
import br.unisagrado.ciclos.analise.Metricas;
import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.Map;

/**
 * Um projeto carregado na aplicação: seus grafos, um por nível de granularidade.
 */
public final class Projeto {

    /** Grafo de pacotes (projetos Java). */
    public static final String NIVEL_PACOTES = "pacotes";
    /** Grafo de classes (projetos Java). */
    public static final String NIVEL_CLASSES = "classes";
    /** Grafo digitado como lista de arestas. */
    public static final String NIVEL_INFORMADO = "informado";

    public record Analise(
            ResultadoMalgrange resultado,
            Metricas metricas
    ) {
    }

    private final String id;
    private final String nome;
    private final Map<String, Digrafo> grafos;

    /** @param grafos nível → grafo; o primeiro nível é o padrão */
    public Projeto(String id, String nome, Map<String, Digrafo> grafos) {
        this.id = id;
        this.nome = nome;
        this.grafos = grafos;
    }

    public String id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public Map<String, Digrafo> grafos() {
        return grafos;
    }

    /** Valida o nível pedido; nulo ou vazio = nível padrão. */
    public String resolverNivel(String pedido) {
        if (pedido == null || pedido.isBlank()) {
            return grafos.keySet().iterator().next();
        }
        if (!grafos.containsKey(pedido)) {
            throw new IllegalArgumentException(
                    "Nível \"" + pedido + "\" não disponível. Disponíveis: " + grafos.keySet());
        }
        return pedido;
    }

    public Digrafo grafo(String nivel) {
        return grafos.get(resolverNivel(nivel));
    }

    /** Executa o Malgrange no grafo do nível e mede o tempo. */
    public Analise analisar(String nivel, boolean registrarPassos) {
        Digrafo grafo = grafo(nivel);

        long inicio = System.nanoTime();
        ResultadoMalgrange resultado = Malgrange.executar(grafo, registrarPassos);
        double milissegundos = (System.nanoTime() - inicio) / 1_000_000.0;

        return new Analise(resultado, Metricas.calcular(grafo, resultado.componentes(), milissegundos));
    }
}
