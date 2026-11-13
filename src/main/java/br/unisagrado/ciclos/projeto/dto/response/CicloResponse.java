package br.unisagrado.ciclos.projeto.dto.response;

import java.util.List;

/** @param ciclo [v0, v1, ..., v0]; vazio se a CFC é trivial (sem ciclo) */
public record CicloResponse(
        String nivel,
        int componente,
        List<String> ciclo,
        List<EvidenciaResponse> arestas
) {
}
