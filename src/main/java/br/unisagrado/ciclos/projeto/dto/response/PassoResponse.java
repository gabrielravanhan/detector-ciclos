package br.unisagrado.ciclos.projeto.dto.response;

import java.util.List;

/**
 * Uma iteração do Malgrange.
 *
 * @param niveisSucessores   níveis de Γ⁺(pivô): [[pivô], Γ¹, novos de Γ², ...]
 * @param niveisAntecessores níveis de Γ⁻(pivô): [[pivô], Γ⁻¹, novos de Γ⁻², ...]
 * @param componente         Γ⁺ ∩ Γ⁻ (CFC encontrada nesta iteração)
 */
public record PassoResponse(
        int iteracao,
        String pivo,
        List<List<String>> niveisSucessores,
        List<List<String>> niveisAntecessores,
        List<String> componente
) {
}
