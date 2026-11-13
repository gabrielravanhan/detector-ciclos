package br.unisagrado.ciclos.api;

import br.unisagrado.ciclos.extracao.DependenciasDeClasses;
import br.unisagrado.ciclos.extracao.ExecutorJdeps;
import br.unisagrado.ciclos.extracao.LeitorSaidaJdeps;
import br.unisagrado.ciclos.extracao.MontadorDeGrafos;
import br.unisagrado.ciclos.grafo.Digrafo;
import br.unisagrado.ciclos.grafo.ListaDeArestas;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Projetos carregados, mantidos em memória enquanto a aplicação está aberta. */
public final class RepositorioDeProjetos {

    private final Map<String, Projeto> projetos = new ConcurrentHashMap<>();
    private final AtomicInteger proximoId = new AtomicInteger(1);

    /** Extrai as dependências de um .jar/.war (ou pasta de .class) e monta os grafos de pacotes e de classes. */
    public Projeto carregarJar(Path jar, String nome) {
        DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler(ExecutorJdeps.executar(jar));
        if (dependencias.classes().isEmpty()) {
            throw new IllegalArgumentException("Nenhuma classe Java encontrada em " + nome);
        }
        Digrafo grafoDeClasses = MontadorDeGrafos.grafoDeClasses(dependencias);

        Map<String, Digrafo> grafos = new LinkedHashMap<>();
        grafos.put(Projeto.NIVEL_PACOTES, MontadorDeGrafos.grafoDePacotes(grafoDeClasses));
        grafos.put(Projeto.NIVEL_CLASSES, grafoDeClasses);
        return registrar(nome, "jar", grafos);
    }

    public Projeto carregarListaDeArestas(String texto, String nome) {
        Map<String, Digrafo> grafos = new LinkedHashMap<>();
        grafos.put(Projeto.NIVEL_INFORMADO, ListaDeArestas.ler(texto));
        return registrar(nome, "lista de arestas", grafos);
    }

    public Projeto buscar(String id) {
        Projeto projeto = projetos.get(id);
        if (projeto == null) {
            throw new NoSuchElementException("Grafo \"" + id + "\" não encontrado. Carregue-o novamente.");
        }
        return projeto;
    }

    private Projeto registrar(String nome, String origem, Map<String, Digrafo> grafos) {
        String id = "g" + proximoId.getAndIncrement();
        Projeto projeto = new Projeto(id, nome, origem, grafos);
        projetos.put(id, projeto);
        return projeto;
    }
}
