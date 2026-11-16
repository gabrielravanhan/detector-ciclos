package br.unisagrado.ciclos.algoritmo;

import java.util.List;

/**
 * Resultado do Malgrange.
 *
 * @param componentes as CFCs na ordem em que foram encontradas; a CFC i saiu da iteração i
 * @param passos      detalhes de cada iteração (vazio se o registro de passos estava desligado)
 */
public record ResultadoMalgrange(
        List<List<Integer>> componentes,
        List<PassoMalgrange> passos
) {
}
