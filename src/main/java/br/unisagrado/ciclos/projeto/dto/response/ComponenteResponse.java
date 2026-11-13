package br.unisagrado.ciclos.projeto.dto.response;

import java.util.List;

/** @param id índice da CFC = iteração do Malgrange em que ela foi encontrada */
public record ComponenteResponse(
        int id,
        int tamanho,
        List<String> vertices
) {
}
