package br.unisagrado.ciclos.instancia;

import br.unisagrado.ciclos.instancia.dto.response.InstanciaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/instancias: instâncias prontas que a interface oferece para carregar. */
@RestController
@RequestMapping("/api/instancias")
public class InstanciaController {

    private final InstanciaService instanciaService;

    public InstanciaController(InstanciaService instanciaService) {
        this.instanciaService = instanciaService;
    }

    @GetMapping
    public List<InstanciaResponse> listar() {
        return instanciaService.listar();
    }
}
