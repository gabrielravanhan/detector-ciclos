package br.unisagrado.ciclos.projeto.dto.response;

/** @param nivel "pacotes", "classes" ou "informado" */
public record NivelResponse(
        String nivel,
        int vertices,
        int arestas
) {
}
