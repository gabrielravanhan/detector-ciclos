package br.unisagrado.ciclos.projeto.dto.response;

import br.unisagrado.ciclos.analise.Metricas;

import java.util.List;

/** @param observacaoPassos explica por que os passos não vieram, quando for o caso */
public record MalgrangeResponse(
        String nivel,
        Metricas metricas,
        List<ComponenteResponse> componentes,
        List<PassoResponse> passos,
        String observacaoPassos
) {
}
