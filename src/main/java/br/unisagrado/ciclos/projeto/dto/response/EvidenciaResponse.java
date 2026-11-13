package br.unisagrado.ciclos.projeto.dto.response;

import java.util.List;

/** @param dependenciasDeClasses pares "ClasseA -> ClasseB" que justificam uma aresta entre pacotes */
public record EvidenciaResponse(
        String origem,
        String destino,
        int peso,
        List<String> dependenciasDeClasses
) {
}
