package br.unisagrado.ciclos.projeto;

import br.unisagrado.ciclos.grafo.ListaDeArestas;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjetoTest {

    private final Projeto projeto = new Projeto("g1", "ciclo", "lista de arestas",
            new LinkedHashMap<>(Map.of(Projeto.NIVEL_INFORMADO, ListaDeArestas.ler("a -> b\nb -> a"))));

    @Test
    void primeiroNivelEhOPadrao() {
        assertEquals(Projeto.NIVEL_INFORMADO, projeto.resolverNivel(null));
        assertEquals(Projeto.NIVEL_INFORMADO, projeto.resolverNivel(" "));
        assertThrows(IllegalArgumentException.class, () -> projeto.resolverNivel(Projeto.NIVEL_PACOTES));
    }

    @Test
    void analisarExecutaOMalgrangeNoNivelPedido() {
        Projeto.Analise analise = projeto.analisar(null, false);

        assertEquals(1, analise.resultado().componentes().size(), "a e b se usam: uma única CFC");
        assertEquals(1, analise.metricas().componentesComCiclo());
    }
}
