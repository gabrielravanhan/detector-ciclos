package br.unisagrado.ciclos.projeto.dto.response;

/** Uma CFC vista como um único vértice do grafo condensado. */
public record VerticeCondensadoResponse(
        String id,
        int componente,
        int tamanho,
        int camada,
        String rotulo
) {
}
