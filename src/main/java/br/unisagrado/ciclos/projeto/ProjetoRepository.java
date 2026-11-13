package br.unisagrado.ciclos.projeto;

import br.unisagrado.ciclos.grafo.Digrafo;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Projetos carregados, guardados em memória enquanto a aplicação está aberta (não há banco de dados).
 * Os ids são "g1", "g2", ...
 */
@Repository
public class ProjetoRepository {

    private final Map<String, Projeto> projetos = new ConcurrentHashMap<>();
    private final AtomicInteger proximoId = new AtomicInteger(1);

    /** @param grafos nível → grafo; o primeiro nível é o padrão */
    public Projeto criar(String nome, String origem, Map<String, Digrafo> grafos) {
        String id = "g" + proximoId.getAndIncrement();
        Projeto projeto = new Projeto(id, nome, origem, grafos);
        projetos.put(id, projeto);
        return projeto;
    }

    public Optional<Projeto> buscarPorId(String id) {
        return Optional.ofNullable(projetos.get(id));
    }
}
