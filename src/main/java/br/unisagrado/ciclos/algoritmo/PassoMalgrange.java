package br.unisagrado.ciclos.algoritmo;

import java.util.List;

/**
 * Registro de uma iteração do Malgrange, usado na exibição passo a passo.
 *
 * @param pivo               vértice escolhido nesta iteração
 * @param niveisSucessores   níveis de Γ⁺(pivô); o nível 0 é o próprio pivô
 * @param niveisAntecessores níveis de Γ⁻(pivô); o nível 0 é o próprio pivô
 * @param componente         Γ⁺(pivô) ∩ Γ⁻(pivô), a CFC encontrada (removida em seguida)
 */
public record PassoMalgrange(
        int pivo,
        List<List<Integer>> niveisSucessores,
        List<List<Integer>> niveisAntecessores,
        List<Integer> componente) {
}
