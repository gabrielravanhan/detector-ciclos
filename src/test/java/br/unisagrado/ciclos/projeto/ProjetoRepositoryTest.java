package br.unisagrado.ciclos.projeto;

import br.unisagrado.ciclos.grafo.Digrafo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjetoRepositoryTest {

    private final ProjetoRepository projetoRepository = new ProjetoRepository();

    @Test
    void cadaProjetoRecebeUmIdEPodeSerBuscado() {
        Projeto primeiro = projetoRepository.criar("1", Map.of(Projeto.NIVEL_INFORMADO, new Digrafo()));
        Projeto segundo = projetoRepository.criar("2", Map.of(Projeto.NIVEL_INFORMADO, new Digrafo()));

        assertNotEquals(primeiro.id(), segundo.id());
        assertSame(segundo, projetoRepository.buscarPorId(segundo.id()).orElseThrow());
    }

    @Test
    void idInexistenteDevolveVazio() {
        assertTrue(projetoRepository.buscarPorId("g999").isEmpty());
    }
}
