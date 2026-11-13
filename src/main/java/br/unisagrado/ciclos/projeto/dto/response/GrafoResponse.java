package br.unisagrado.ciclos.projeto.dto.response;

import java.util.List;

/** Vértices e arestas de um nível; os vértices são identificados pelo nome. */
public record GrafoResponse(
        String nivel,
        List<VerticeResponse> vertices,
        List<ArestaResponse> arestas
) {
}
