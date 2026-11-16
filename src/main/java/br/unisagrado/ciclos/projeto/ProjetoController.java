package br.unisagrado.ciclos.projeto;

import br.unisagrado.ciclos.projeto.dto.response.GrafoResponse;
import br.unisagrado.ciclos.projeto.dto.response.MalgrangeResponse;
import br.unisagrado.ciclos.projeto.dto.response.ProjetoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/**
 * Rotas dos projetos carregados. O parâmetro nivel é "pacotes", "classes" ou "informado";
 * se omitido, usa o primeiro nível do projeto.
 *
 * <pre>
 * POST /api/projetos/instancia/{nome}                    carrega uma instância pronta
 * POST /api/projetos/envio?arquivo=x.jar                 carrega um .jar/.war (corpo = bytes do arquivo)
 * POST /api/projetos/arestas?nome=...                    carrega uma lista de arestas (corpo = texto)
 * GET  /api/projetos/{id}/grafo?nivel=                   vértices e arestas
 * GET  /api/projetos/{id}/malgrange?nivel=&amp;passos=true      executa o Malgrange
 * </pre>
 */
@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @PostMapping("/instancia/{nome}")
    public ProjetoResponse carregarInstancia(@PathVariable String nome) throws IOException {
        return projetoService.carregarInstancia(nome);
    }

    /** O navegador manda o arquivo inteiro no corpo da requisição (sem formulário multipart). */
    @PostMapping("/envio")
    public ProjetoResponse carregarArquivo(@RequestParam(required = false) String arquivo, InputStream corpo)
            throws IOException {
        return projetoService.carregarArquivo(arquivo, corpo);
    }

    @PostMapping("/arestas")
    public ProjetoResponse carregarArestas(@RequestParam(required = false) String nome,
                                           @RequestBody(required = false) String texto) {
        return projetoService.carregarArestas(texto, nome);
    }

    @GetMapping("/{id}/grafo")
    public GrafoResponse buscarGrafo(@PathVariable String id, @RequestParam(required = false) String nivel) {
        return projetoService.buscarGrafo(id, nivel);
    }

    /** GET: a execução só calcula e devolve; nada é guardado no servidor. */
    @GetMapping("/{id}/malgrange")
    public MalgrangeResponse executarMalgrange(@PathVariable String id,
                                               @RequestParam(required = false) String nivel,
                                               @RequestParam(defaultValue = "false") boolean passos) {
        return projetoService.executarMalgrange(id, nivel, passos);
    }
}
