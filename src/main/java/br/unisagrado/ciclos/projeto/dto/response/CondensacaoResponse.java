package br.unisagrado.ciclos.projeto.dto.response;

import java.util.List;

/** @param ordemTopologica ids das CFCs; se A → B, A aparece antes de B */
public record CondensacaoResponse(
        String nivel,
        List<VerticeCondensadoResponse> vertices,
        List<ArestaResponse> arestas,
        List<String> ordemTopologica
) {
}
