package br.unisagrado.ciclos.api;

import br.unisagrado.ciclos.algoritmo.Malgrange;
import br.unisagrado.ciclos.algoritmo.ResultadoMalgrange;
import br.unisagrado.ciclos.analise.Metricas;
import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Um projeto carregado na aplicação: seus grafos (um por nível de granularidade) e a última
 * análise feita em cada nível.
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
    private final String origem;
    private final Map<String, Digrafo> grafos;
    private final Map<String, Analise> analises = new ConcurrentHashMap<>();

    /** @param grafos nível → grafo; o primeiro nível é o padrão */
    public Projeto(String id, String nome, String origem, Map<String, Digrafo> grafos) {
        this.id = id;
        this.nome = nome;
        this.origem = origem;
        this.grafos = grafos;
    }

    public String id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String origem() {
        return origem;
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

    /** Grafo de classes, ou nulo se o projeto não veio de um .jar. */
    public Digrafo grafoDeClasses() {
        return grafos.get(NIVEL_CLASSES);
    }

    /** Executa o Malgrange, mede o tempo e guarda o resultado como a análise mais recente do nível. */
    public Analise analisar(String nivel, boolean registrarPassos) {
        String nivelResolvido = resolverNivel(nivel);
        Digrafo grafo = grafos.get(nivelResolvido);

        long inicio = System.nanoTime();
        ResultadoMalgrange resultado = Malgrange.executar(grafo, registrarPassos);
        double milissegundos = (System.nanoTime() - inicio) / 1_000_000.0;

        Analise analise = new Analise(resultado, Metricas.calcular(grafo, resultado.componentes(), milissegundos));
        analises.put(nivelResolvido, analise);
        return analise;
    }

    /** Análise mais recente do nível; executa o Malgrange se ainda não houver uma. */
    public Analise ultimaAnalise(String nivel) {
        String nivelResolvido = resolverNivel(nivel);
        Analise existente = analises.get(nivelResolvido);
        return existente != null ? existente : analisar(nivelResolvido, false);
    }
}
