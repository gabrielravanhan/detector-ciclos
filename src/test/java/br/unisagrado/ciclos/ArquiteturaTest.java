package br.unisagrado.ciclos;

import br.unisagrado.ciclos.algoritmo.Malgrange;
import br.unisagrado.ciclos.extracao.ExecutorJdeps;
import br.unisagrado.ciclos.extracao.LeitorSaidaJdeps;
import br.unisagrado.ciclos.extracao.MontadorDeGrafos;
import br.unisagrado.ciclos.grafo.Digrafo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** O detector analisa o próprio código: os pacotes do projeto não podem formar ciclo. */
class ArquiteturaTest {

    @Test
    void pacotesDoProjetoNaoTemCiclos() {
        Digrafo classes = MontadorDeGrafos.grafoDeClasses(
                LeitorSaidaJdeps.ler(ExecutorJdeps.executar(Path.of("target/classes"))));
        Digrafo pacotes = MontadorDeGrafos.grafoDePacotes(classes);

        List<List<Integer>> componentes = Malgrange.executar(pacotes, false).componentes();

        assertTrue(pacotes.quantidadeVertices() >= 4, "grafo, algoritmo, analise e extracao");
        for (List<Integer> componente : componentes) {
            assertTrue(componente.size() == 1, "ciclo entre pacotes do projeto: "
                    + componente.stream().map(pacotes::nome).toList());
        }
    }
}
