package br.unisagrado.ciclos.projeto;

import br.unisagrado.ciclos.algoritmo.PassoMalgrange;
import br.unisagrado.ciclos.common.exception.NaoEncontradoException;
import br.unisagrado.ciclos.extracao.DependenciasDeClasses;
import br.unisagrado.ciclos.extracao.ExecutorJdeps;
import br.unisagrado.ciclos.extracao.LeitorSaidaJdeps;
import br.unisagrado.ciclos.extracao.MontadorDeGrafos;
import br.unisagrado.ciclos.grafo.Digrafo;
import br.unisagrado.ciclos.grafo.ListaDeArestas;
import br.unisagrado.ciclos.instancia.InstanciaService;
import br.unisagrado.ciclos.projeto.dto.response.ArestaResponse;
import br.unisagrado.ciclos.projeto.dto.response.ComponenteResponse;
import br.unisagrado.ciclos.projeto.dto.response.GrafoResponse;
import br.unisagrado.ciclos.projeto.dto.response.MalgrangeResponse;
import br.unisagrado.ciclos.projeto.dto.response.NivelResponse;
import br.unisagrado.ciclos.projeto.dto.response.PassoResponse;
import br.unisagrado.ciclos.projeto.dto.response.ProjetoResponse;
import br.unisagrado.ciclos.projeto.dto.response.VerticeResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrega projetos (instância pronta, .jar enviado ou lista de arestas) e responde às análises:
 * grafo e Malgrange.
 */
@Service
public class ProjetoService {

    /** Acima disso, os passos não são registrados (a memória cresce O(V²)). */
    static final int LIMITE_PARA_REGISTRAR_PASSOS = 500;

    private final ProjetoRepository projetoRepository;
    private final InstanciaService instanciaService;

    public ProjetoService(ProjetoRepository projetoRepository, InstanciaService instanciaService) {
        this.projetoRepository = projetoRepository;
        this.instanciaService = instanciaService;
    }

    // ---------- carregamento ----------

    public ProjetoResponse carregarInstancia(String nome) throws IOException {
        Path arquivo = instanciaService.resolver(nome);
        return InstanciaService.ehArquivoCompilado(nome)
                ? carregarJar(arquivo, nome)
                : carregarArestas(Files.readString(arquivo), nome);
    }

    /** O arquivo enviado pelo navegador é salvo num temporário, analisado e apagado. */
    public ProjetoResponse carregarArquivo(String nomeDoArquivo, InputStream conteudo) throws IOException {
        if (nomeDoArquivo == null || !InstanciaService.ehArquivoCompilado(nomeDoArquivo)) {
            throw new IllegalArgumentException("Envie um arquivo .jar ou .war (parâmetro arquivo).");
        }
        Path temporario = Files.createTempFile("envio-", ".jar");
        try {
            Files.copy(conteudo, temporario, StandardCopyOption.REPLACE_EXISTING);
            return carregarJar(temporario, nomeDoArquivo);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    /** Extrai as dependências de um .jar/.war (ou pasta de .class) e monta os grafos de pacotes e de classes. */
    public ProjetoResponse carregarJar(Path jar, String nome) {
        DependenciasDeClasses dependencias = LeitorSaidaJdeps.ler(ExecutorJdeps.executar(jar));
        if (dependencias.classes().isEmpty()) {
            throw new IllegalArgumentException("Nenhuma classe Java encontrada em " + nome);
        }
        Digrafo grafoDeClasses = MontadorDeGrafos.grafoDeClasses(dependencias);

        Map<String, Digrafo> grafos = new LinkedHashMap<>();
        grafos.put(Projeto.NIVEL_PACOTES, MontadorDeGrafos.grafoDePacotes(grafoDeClasses));
        grafos.put(Projeto.NIVEL_CLASSES, grafoDeClasses);
        return resumo(projetoRepository.criar(nome, grafos));
    }

    public ProjetoResponse carregarArestas(String texto, String nome) {
        String nomeFinal = nome == null || nome.isBlank() ? "grafo manual" : nome;
        Map<String, Digrafo> grafos = new LinkedHashMap<>();
        grafos.put(Projeto.NIVEL_INFORMADO, ListaDeArestas.ler(texto == null ? "" : texto));
        return resumo(projetoRepository.criar(nomeFinal, grafos));
    }

    // ---------- consultas ----------

    public GrafoResponse buscarGrafo(String id, String nivelPedido) {
        Projeto projeto = buscarProjeto(id);
        String nivel = projeto.resolverNivel(nivelPedido);
        Digrafo grafo = projeto.grafo(nivel);

        List<VerticeResponse> vertices = new ArrayList<>();
        for (int v = 0; v < grafo.quantidadeVertices(); v++) {
            vertices.add(vertice(grafo.nome(v), nivel));
        }
        return new GrafoResponse(nivel, vertices, arestas(grafo));
    }

    /** Executa o Malgrange; os passos só são registrados para grafos de até LIMITE_PARA_REGISTRAR_PASSOS vértices. */
    public MalgrangeResponse executarMalgrange(String id, String nivelPedido, boolean passosPedidos) {
        Projeto projeto = buscarProjeto(id);
        String nivel = projeto.resolverNivel(nivelPedido);
        Digrafo grafo = projeto.grafo(nivel);
        boolean registrarPassos = passosPedidos && grafo.quantidadeVertices() <= LIMITE_PARA_REGISTRAR_PASSOS;

        Projeto.Analise analise = projeto.analisar(nivel, registrarPassos);

        List<ComponenteResponse> componentes = new ArrayList<>();
        List<List<Integer>> encontradas = analise.resultado().componentes();
        for (int c = 0; c < encontradas.size(); c++) {
            componentes.add(new ComponenteResponse(c, encontradas.get(c).size(), nomes(grafo, encontradas.get(c))));
        }

        List<PassoResponse> passos = new ArrayList<>();
        int iteracao = 0;
        for (PassoMalgrange passo : analise.resultado().passos()) {
            passos.add(new PassoResponse(iteracao++, grafo.nome(passo.pivo()),
                    nomesDosNiveis(grafo, passo.niveisSucessores()),
                    nomesDosNiveis(grafo, passo.niveisAntecessores()),
                    nomes(grafo, passo.componente())));
        }
        String observacao = passosPedidos && !registrarPassos
                ? "Passos omitidos: o grafo tem " + grafo.quantidadeVertices() + " vértices (limite "
                  + LIMITE_PARA_REGISTRAR_PASSOS + ")."
                : null;

        return new MalgrangeResponse(nivel, analise.metricas(), componentes, passos, observacao);
    }

    // ---------- auxiliares ----------

    private Projeto buscarProjeto(String id) {
        return projetoRepository.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Projeto \"" + id + "\" não encontrado. Carregue-o novamente."));
    }

    private static ProjetoResponse resumo(Projeto projeto) {
        List<NivelResponse> niveis = new ArrayList<>();
        for (Map.Entry<String, Digrafo> entrada : projeto.grafos().entrySet()) {
            Digrafo grafo = entrada.getValue();
            niveis.add(new NivelResponse(entrada.getKey(), grafo.quantidadeVertices(), grafo.quantidadeArestas()));
        }
        return new ProjetoResponse(projeto.id(), projeto.nome(), niveis);
    }

    private static VerticeResponse vertice(String nome, String nivel) {
        if (Projeto.NIVEL_CLASSES.equals(nivel)) {
            String nomeSimples = nome.substring(nome.lastIndexOf('.') + 1);
            return new VerticeResponse(nome, nomeSimples, MontadorDeGrafos.pacoteDe(nome));
        }
        return new VerticeResponse(nome, nome, null);
    }

    private static List<ArestaResponse> arestas(Digrafo grafo) {
        List<ArestaResponse> arestas = new ArrayList<>();
        for (int v = 0; v < grafo.quantidadeVertices(); v++) {
            for (int w : grafo.sucessores(v)) {
                arestas.add(new ArestaResponse(grafo.nome(v), grafo.nome(w)));
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
