package br.unisagrado.ciclos.common.exception.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/** Corpo de toda resposta de erro da API. */
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path
) {
    // O "erro" é a descrição padrão do status (ex.: 404 → "Not Found").
    public ErroResponse(HttpStatus status, String mensagem, String path) {
        this(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path);
    }
}
