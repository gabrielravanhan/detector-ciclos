package br.unisagrado.ciclos.api;

import br.unisagrado.ciclos.grafo.ListaDeArestas;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Rotas da API REST e arquivos da interface web. As respostas são montadas por {@link Respostas}.
 *
 * <pre>
 * GET  /api/instancias                              instâncias prontas
 * POST /api/grafos/instancia/{nome}                 carrega uma instância pronta
 * POST /api/grafos/envio?arquivo=x.jar              carrega um .jar/.war (corpo = bytes do arquivo)
 * POST /api/grafos/arestas?nome=...                 carrega uma lista de arestas (corpo = texto)
 * GET  /api/grafos/{id}?nivel=                      vértices e arestas
 * GET  /api/grafos/{id}/exportar?nivel=             o grafo como lista de arestas (texto)
 * POST /api/grafos/{id}/malgrange?nivel=&amp;passos=true   executa o Malgrange
 * GET  /api/grafos/{id}/condensacao?nivel=          grafo condensado (DAG das CFCs)
 * GET  /api/grafos/{id}/componentes/{c}/ciclo?nivel=&amp;inicio=   ciclo concreto na CFC c
 * </pre>
 * O parâmetro nivel é "pacotes", "classes" ou "informado"; se omitido, usa o primeiro disponível.
 */
public final class ServidorApi {

    private final RepositorioDeProjetos repositorio;
    private final CatalogoDeInstancias catalogo;

    public ServidorApi(RepositorioDeProjetos repositorio, CatalogoDeInstancias catalogo) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
    }

    public Javalin criar() {
        Javalin app = Javalin.create(configuracao -> {
            configuracao.showJavalinBanner = false;
            configuracao.http.maxRequestSize = 50_000_000L;
            // a interface (index.html, style.css, app.js) fica em src/main/resources/public
            configuracao.staticFiles.add("/public", Location.CLASSPATH);
            // bibliotecas JavaScript empacotadas como jar (Cytoscape.js), servidas em /webjars/...
            configuracao.staticFiles.enableWebjars();
        });

        app.get("/api/instancias", ctx -> ctx.json(catalogo.listar()));
        app.post("/api/grafos/instancia/{nome}", this::carregarInstancia);
        app.post("/api/grafos/envio", this::receberArquivo);
        app.post("/api/grafos/arestas", this::carregarArestas);
        app.get("/api/grafos/{id}", ctx ->
                ctx.json(Respostas.grafo(projeto(ctx), ctx.queryParam("nivel"))));
        app.get("/api/grafos/{id}/exportar", ctx ->
                ctx.contentType("text/plain; charset=utf-8")
                        .result(ListaDeArestas.escrever(projeto(ctx).grafo(ctx.queryParam("nivel")))));
        app.post("/api/grafos/{id}/malgrange", ctx ->
                ctx.json(Respostas.malgrange(projeto(ctx), ctx.queryParam("nivel"),
                        ctx.queryParamAsClass("passos", Boolean.class).getOrDefault(false))));
        app.get("/api/grafos/{id}/condensacao", ctx ->
                ctx.json(Respostas.condensacao(projeto(ctx), ctx.queryParam("nivel"))));
        app.get("/api/grafos/{id}/componentes/{componente}/ciclo", ctx ->
                ctx.json(Respostas.ciclo(projeto(ctx), ctx.queryParam("nivel"),
                        ctx.pathParamAsClass("componente", Integer.class).get(), ctx.queryParam("inicio"))));

        app.exception(IllegalArgumentException.class, (e, ctx) -> erro(ctx, 400, e.getMessage()));
        app.exception(NoSuchElementException.class, (e, ctx) -> erro(ctx, 404, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            erro(ctx, 500, "Erro interno: " + e.getMessage());
        });
        return app;
    }

    private Projeto projeto(Context ctx) {
        return repositorio.buscar(ctx.pathParam("id"));
    }

    private void carregarInstancia(Context ctx) throws IOException {
        String nome = ctx.pathParam("nome");
        Path arquivo = catalogo.resolver(nome);
        Projeto projeto = CatalogoDeInstancias.ehArquivoCompilado(nome)
                ? repositorio.carregarJar(arquivo, nome)
                : repositorio.carregarListaDeArestas(Files.readString(arquivo), nome);
        ctx.json(Respostas.resumo(projeto));
    }

    private void receberArquivo(Context ctx) throws IOException {
        String nome = ctx.queryParam("arquivo");
        if (nome == null || !CatalogoDeInstancias.ehArquivoCompilado(nome)) {
            throw new IllegalArgumentException("Envie um arquivo .jar ou .war (parâmetro arquivo).");
        }
        Path temporario = Files.createTempFile("envio-", ".jar");
        try (InputStream corpo = ctx.bodyInputStream()) {
            Files.copy(corpo, temporario, StandardCopyOption.REPLACE_EXISTING);
            ctx.json(Respostas.resumo(repositorio.carregarJar(temporario, nome)));
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private void carregarArestas(Context ctx) {
        String nome = ctx.queryParam("nome");
        String nomeFinal = nome == null || nome.isBlank() ? "grafo manual" : nome;
        ctx.json(Respostas.resumo(repositorio.carregarListaDeArestas(ctx.body(), nomeFinal)));
    }

    private static void erro(Context ctx, int status, String mensagem) {
        ctx.status(status).json(Map.of("erro", mensagem == null ? "erro desconhecido" : mensagem));
    }
}
