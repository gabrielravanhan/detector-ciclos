package br.unisagrado.ciclos.projeto.dto.response;

import java.util.List;

/** Projeto carregado: o id usado nas outras rotas e os níveis de grafo disponíveis. */
public record ProjetoResponse(
        String id,
        String nome,
        List<NivelResponse> niveis
) {
}
