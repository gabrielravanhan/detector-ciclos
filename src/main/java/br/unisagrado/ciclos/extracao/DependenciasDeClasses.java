package br.unisagrado.ciclos.extracao;

import java.util.List;
import java.util.Set;

/**
 * Dependências entre classes de um mesmo projeto, já filtradas.
 *
 * @param classes      todas as classes de nível superior do projeto (classes internas, como
 *                     {@code Foo$Bar}, já foram agrupadas em {@code Foo})
 * @param dependencias pares "classe de origem usa classe de destino"; os dois lados pertencem
 *                     a {@code classes}. Pares repetidos são mantidos e viram peso no grafo.
 */
public record DependenciasDeClasses(
        Set<String> classes,
        List<Dependencia> dependencias
) {

    public record Dependencia(
            String origem,
            String destino
    ) {
    }
}
