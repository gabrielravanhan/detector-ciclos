package br.unisagrado.ciclos.api;

import br.unisagrado.ciclos.algoritmo.PassoMalgrange;
import br.unisagrado.ciclos.analise.BuscadorDeCiclos;
import br.unisagrado.ciclos.analise.Condensacao;
import br.unisagrado.ciclos.api.ModelosJson.ArestaJson;
import br.unisagrado.ciclos.api.ModelosJson.CicloJson;
import br.unisagrado.ciclos.api.ModelosJson.ComponenteJson;
import br.unisagrado.ciclos.api.ModelosJson.CondensacaoJson;
import br.unisagrado.ciclos.api.ModelosJson.EvidenciaJson;
import br.unisagrado.ciclos.api.ModelosJson.GrafoJson;
import br.unisagrado.ciclos.api.ModelosJson.PassoJson;
import br.unisagrado.ciclos.api.ModelosJson.ResultadoMalgrangeJson;
import br.unisagrado.ciclos.api.ModelosJson.ResumoDoGrafo;
import br.unisagrado.ciclos.api.ModelosJson.ResumoDoNivel;
import br.unisagrado.ciclos.api.ModelosJson.VerticeCondensadoJson;
import br.unisagrado.ciclos.api.ModelosJson.VerticeJson;
import br.unisagrado.ciclos.extracao.MontadorDeGrafos;
import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Monta as respostas da API a partir dos projetos carregados. Fica separado das rotas
 * ({@link ServidorApi}) para poder ser testado sem subir o servidor HTTP.
 */
public final class Respostas {

    /** Acima disso, os passos não são registrados (a memória cresce O(V²)). */
    static final int LIMITE_PARA_REGISTRAR_PASSOS = 500;
    private static final int LIMITE_DE_EVIDENCIAS = 50;

    private Respostas() {
    }

    public static ResumoDoGrafo resumo(Projeto projeto) {
        List<ResumoDoNivel> niveis = new ArrayList<>();
        for (Map.Entry<String, Digrafo> entrada : projeto.grafos().entrySet()) {
            Digrafo grafo = entrada.getValue();
            niveis.add(new ResumoDoNivel(entrada.getKey(), grafo.quantidadeVertices(), grafo.quantidadeArestas()));
        }
        return new ResumoDoGrafo(projeto.id(), projeto.nome(), projeto.origem(), niveis);
    }

    public static GrafoJson grafo(Projeto projeto, String nivelPedido) {
        String nivel = projeto.resolverNivel(nivelPedido);
        Digrafo grafo = projeto.grafo(nivel);

        List<VerticeJson> vertices = new ArrayList<>();
        for (int v = 0; v < grafo.quantidadeVertices(); v++) {
            vertices.add(vertice(grafo.nome(v), nivel));
        }
        return new GrafoJson(nivel, vertices, arestas(grafo));
    }

    /** Executa o Malgrange; os passos só são registrados para grafos de até LIMITE_PARA_REGISTRAR_PASSOS vértices. */
    public static ResultadoMalgrangeJson malgrange(Projeto projeto, String nivelPedido, boolean passosPedidos) {
        String nivel = projeto.resolverNivel(nivelPedido);
        Digrafo grafo = projeto.grafo(nivel);
        boolean registrarPassos = passosPedidos && grafo.quantidadeVertices() <= LIMITE_PARA_REGISTRAR_PASSOS;

        Projeto.Analise analise = projeto.analisar(nivel, registrarPassos);

        List<ComponenteJson> componentes = new ArrayList<>();
        List<List<Integer>> encontradas = analise.resultado().componentes();
        for (int c = 0; c < encontradas.size(); c++) {
            componentes.add(new ComponenteJson(c, encontradas.get(c).size(), nomes(grafo, encontradas.get(c))));
        }

        List<PassoJson> passos = new ArrayList<>();
        int iteracao = 0;
        for (PassoMalgrange passo : analise.resultado().passos()) {
            passos.add(new PassoJson(iteracao++, grafo.nome(passo.pivo()),
                    nomesDosNiveis(grafo, passo.niveisSucessores()),
                    nomesDosNiveis(grafo, passo.niveisAntecessores()),
                    nomes(grafo, passo.componente())));
        }
        String observacao = passosPedidos && !registrarPassos
                ? "Passos omitidos: o grafo tem " + grafo.quantidadeVertices() + " vértices (limite "
                  + LIMITE_PARA_REGISTRAR_PASSOS + ")."
                : null;

        return new ResultadoMalgrangeJson(nivel, analise.metricas(), componentes, passos, observacao);
    }

    public static CondensacaoJson condensacao(Projeto projeto, String nivelPedido) {
        String nivel = projeto.resolverNivel(nivelPedido);
        Digrafo grafo = projeto.grafo(nivel);
        List<List<Integer>> componentes = projeto.ultimaAnalise(nivel).resultado().componentes();
        Condensacao condensacao = Condensacao.construir(grafo, componentes);

        List<VerticeCondensadoJson> vertices = new ArrayList<>();
        for (int c = 0; c < componentes.size(); c++) {
            List<Integer> membros = componentes.get(c);
            String rotulo = membros.size() == 1
                    ? grafo.nome(membros.get(0))
                    : "CFC " + c + " (" + membros.size() + " vértices)";
            vertices.add(new VerticeCondensadoJson("c" + c, c, membros.size(), condensacao.camada()[c], rotulo));
        }
        List<ArestaJson> arestas = new ArrayList<>();
        for (Condensacao.Aresta aresta : condensacao.arestas()) {
            arestas.add(new ArestaJson("c" + aresta.origem(), "c" + aresta.destino(), aresta.peso()));
        }
        List<String> ordem = condensacao.ordemTopologica().stream().map(c -> "c" + c).toList();
        return new CondensacaoJson(nivel, vertices, arestas, ordem);
    }

    /**
     * Menor ciclo dentro da CFC {@code componente}, passando por {@code inicio} (ou pelo primeiro
     * vértice da CFC), com as dependências entre classes que justificam cada aresta.
     */
    public static CicloJson ciclo(Projeto projeto, String nivelPedido, int componente, String inicio) {
        String nivel = projeto.resolverNivel(nivelPedido);
        Digrafo grafo = projeto.grafo(nivel);
        List<List<Integer>> componentes = projeto.ultimaAnalise(nivel).resultado().componentes();

        if (componente < 0 || componente >= componentes.size()) {
            throw new NoSuchElementException("CFC " + componente + " não existe (há " + componentes.size() + ").");
        }
        List<Integer> membros = componentes.get(componente);
        int verticeInicial = membros.get(0);
        if (inicio != null) {
            verticeInicial = grafo.indiceDe(inicio);
            if (!membros.contains(verticeInicial)) {
                throw new IllegalArgumentException("\"" + inicio + "\" não pertence à CFC " + componente);
            }
        }

        List<Integer> ciclo = BuscadorDeCiclos.menorCicloPassandoPor(grafo, verticeInicial, membros);
        List<EvidenciaJson> evidencias = new ArrayList<>();
        for (int i = 0; i + 1 < ciclo.size(); i++) {
            int origem = ciclo.get(i);
            int destino = ciclo.get(i + 1);
            List<String> classes = Projeto.NIVEL_PACOTES.equals(nivel)
                    ? dependenciasEntrePacotes(projeto.grafoDeClasses(), grafo.nome(origem), grafo.nome(destino))
                    : List.of();
            evidencias.add(new EvidenciaJson(grafo.nome(origem), grafo.nome(destino), grafo.peso(origem, destino), classes));
        }
        return new CicloJson(nivel, componente, nomes(grafo, ciclo), evidencias);
    }

    /** Pares de classes que geram a aresta entre dois pacotes (até LIMITE_DE_EVIDENCIAS). */
    private static List<String> dependenciasEntrePacotes(Digrafo grafoDeClasses, String pacoteOrigem, String pacoteDestino) {
        List<String> pares = new ArrayList<>();
        for (int a = 0; a < grafoDeClasses.quantidadeVertices() && pares.size() < LIMITE_DE_EVIDENCIAS; a++) {
            if (!MontadorDeGrafos.pacoteDe(grafoDeClasses.nome(a)).equals(pacoteOrigem)) {
                continue;
            }
            for (int b : grafoDeClasses.sucessores(a)) {
                boolean destinoNoPacote = MontadorDeGrafos.pacoteDe(grafoDeClasses.nome(b)).equals(pacoteDestino);
                if (destinoNoPacote && pares.size() < LIMITE_DE_EVIDENCIAS) {
                    pares.add(grafoDeClasses.nome(a) + " -> " + grafoDeClasses.nome(b));
                }
            }
        }
        return pares;
    }

    private static VerticeJson vertice(String nome, String nivel) {
        if (Projeto.NIVEL_CLASSES.equals(nivel)) {
            String nomeSimples = nome.substring(nome.lastIndexOf('.') + 1);
            return new VerticeJson(nome, nomeSimples, MontadorDeGrafos.pacoteDe(nome));
        }
        return new VerticeJson(nome, nome, null);
    }

    private static List<ArestaJson> arestas(Digrafo grafo) {
        List<ArestaJson> arestas = new ArrayList<>();
        for (int v = 0; v < grafo.quantidadeVertices(); v++) {
            for (int w : grafo.sucessores(v)) {
                arestas.add(new ArestaJson(grafo.nome(v), grafo.nome(w), grafo.peso(v, w)));
            }
        }
        return arestas;
    }

    private static List<String> nomes(Digrafo grafo, List<Integer> vertices) {
        return vertices.stream().map(grafo::nome).toList();
    }

    private static List<List<String>> nomesDosNiveis(Digrafo grafo, List<List<Integer>> niveis) {
        return niveis.stream().map(nivel -> nomes(grafo, nivel)).toList();
    }
}
