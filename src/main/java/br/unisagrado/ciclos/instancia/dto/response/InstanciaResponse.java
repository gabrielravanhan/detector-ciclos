package br.unisagrado.ciclos.instancia.dto.response;

/** @param tipo "jar" (.jar/.war analisado com o jdeps) ou "arestas" (.txt com lista de arestas) */
public record InstanciaResponse(
        String nome,
        String tipo,
        long tamanhoEmBytes
) {
}
