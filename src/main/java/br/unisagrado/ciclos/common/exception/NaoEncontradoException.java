package br.unisagrado.ciclos.common.exception;

/** Projeto, instância ou componente pedido não existe. Respondido com 404. */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
