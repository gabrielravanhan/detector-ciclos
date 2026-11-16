package br.unisagrado.ciclos.projeto;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa as rotas pelo HTTP simulado do Spring (MockMvc), sem abrir uma porta de rede.
 * As instâncias vêm de src/test/resources/instancias-de-teste.
 */
@SpringBootTest(properties = "instancias=src/test/resources/instancias-de-teste")
@AutoConfigureMockMvc
class ProjetoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void instanciaCarregadaPassaPeloMalgrangeECiclo() throws Exception {
        String projeto = "/api/projetos/" + carregar(post("/api/projetos/instancia/exemplo-sala.txt"));

        mockMvc.perform(get(projeto + "/grafo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vertices", hasSize(9)));
        mockMvc.perform(post(projeto + "/malgrange").param("passos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas.componentes").value(4))
                .andExpect(jsonPath("$.metricas.componentesComCiclo").value(3))
                .andExpect(jsonPath("$.passos[0].pivo").value("A"))
                .andExpect(jsonPath("$.passos[0].niveisAntecessores[1]", contains("C")));
        mockMvc.perform(get(projeto + "/componentes/0/ciclo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ciclo", contains("A", "B", "C", "A")));
    }

    @Test
    void listaDeArestasVemNoCorpoEPodeSerExportada() throws Exception {
        String id = carregar(post("/api/projetos/arestas").param("nome", "manual")
                .contentType(MediaType.TEXT_PLAIN).content("x -> y\ny -> x 3"));

        mockMvc.perform(get("/api/projetos/" + id + "/exportar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("y -> x 3")));
    }

    @Test
    void errosSaoRespondidosComoJson() throws Exception {
        mockMvc.perform(get("/api/projetos/g999/grafo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem", containsString("g999")))
                .andExpect(jsonPath("$.path").value("/api/projetos/g999/grafo"));
        mockMvc.perform(post("/api/projetos/arestas").contentType(MediaType.TEXT_PLAIN).content("a -> b\na b c d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Linha 2")));
        mockMvc.perform(post("/api/projetos/envio").param("arquivo", "foto.png").content(new byte[] {1}))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/projetos/g1/componentes/abc/ciclo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("componente")));
    }

    @Test
    void interfaceEBibliotecasJavaScriptSaoServidas() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Detector de Ciclos")));
        mockMvc.perform(get("/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/webjars/cytoscape/3.30.2/dist/cytoscape.min.js")).andExpect(status().isOk());
        mockMvc.perform(get("/webjars/cytoscape-fcose/2.2.0/cytoscape-fcose.js")).andExpect(status().isOk());
    }

    /** Faz a requisição de carga e devolve o id do projeto criado. */
    private String carregar(RequestBuilder requisicao) throws Exception {
        String resposta = mockMvc.perform(requisicao)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }
}
