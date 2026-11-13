package br.unisagrado.ciclos.instancia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "instancias=src/test/resources/instancias-de-teste")
@AutoConfigureMockMvc
class InstanciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listaAsInstanciasProntas() throws Exception {
        mockMvc.perform(get("/api/instancias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("exemplo-sala.txt"))
                .andExpect(jsonPath("$[0].tipo").value("arestas"));
    }
}
