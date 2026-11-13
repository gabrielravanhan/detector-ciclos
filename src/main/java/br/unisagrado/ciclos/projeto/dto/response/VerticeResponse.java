package br.unisagrado.ciclos.projeto.dto.response;

/** @param grupo pacote da classe (só no nível de classes); usado para agrupar no desenho */
public record VerticeResponse(
        String id,
        String rotulo,
        String grupo
) {
}
