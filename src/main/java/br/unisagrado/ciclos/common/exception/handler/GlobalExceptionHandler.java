package br.unisagrado.ciclos.common.exception.handler;

import br.unisagrado.ciclos.common.exception.NaoEncontradoException;
import br.unisagrado.ciclos.common.exception.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Transforma as exceções lançadas pelos controllers em respostas JSON ({@link ErroResponse}). */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<ErroResponse> handleNaoEncontrado(NaoEncontradoException exception, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    // Dado inválido: lista de arestas mal escrita, nível inexistente, jar sem classes...
    // As classes do algoritmo (Digrafo, ListaDeArestas) lançam IllegalArgumentException.
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErroResponse> handleDadoInvalido(IllegalArgumentException exception, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    // Parâmetro da URL com tipo errado, ex.: /malgrange?passos=abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> handleParametroInvalido(MethodArgumentTypeMismatchException exception,
                                                         HttpServletRequest request) {
        String mensagem = "Valor inválido para o parâmetro " + exception.getName() + ": " + exception.getValue();
        return responder(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> handleErroInesperado(Exception exception, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), exception);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: " + exception.getMessage(), request);
    }

    private static ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErroResponse(status, mensagem, request.getRequestURI()));
    }
}
