package br.unisagrado.ciclos.extracao;

import br.unisagrado.ciclos.extracao.DependenciasDeClasses.Dependencia;
import br.unisagrado.ciclos.grafo.Digrafo;

import java.util.Set;
import java.util.TreeSet;

/**
 * Monta os grafos de dependência a partir das dependências entre classes.
 *
 * <ul>
 *   <li><b>Grafo de classes:</b> vértice = classe; aresta A → B se A usa B;
 *       peso = número de referências registradas pelo jdeps.</li>
 *   <li><b>Grafo de pacotes:</b> vértice = pacote; aresta P → Q se alguma classe de P usa
 *       alguma classe de Q; peso = número de pares de classes (A em P, B em Q) com A → B.</li>
 * </ul>
 * Os vértices são inseridos em ordem alfabética para que o resultado seja sempre o mesmo.
 */
public final class MontadorDeGrafos {

    public static final String PACOTE_PADRAO = "(pacote padrão)";

    private MontadorDeGrafos() {
    }

    public static Digrafo grafoDeClasses(DependenciasDeClasses dependencias) {
        Digrafo grafo = new Digrafo();
        for (String classe : dependencias.classes()) {
            grafo.adicionarVertice(classe);
        }
        for (Dependencia dependencia : dependencias.dependencias()) {
            grafo.adicionarAresta(dependencia.origem(), dependencia.destino());
        }
        return grafo;
    }

    public static Digrafo grafoDePacotes(Digrafo grafoDeClasses) {
        Set<String> pacotes = new TreeSet<>();
        for (int c = 0; c < grafoDeClasses.quantidadeVertices(); c++) {
            pacotes.add(pacoteDe(grafoDeClasses.nome(c)));
        }

        Digrafo grafo = new Digrafo();
        for (String pacote : pacotes) {
            grafo.adicionarVertice(pacote);
        }
        for (int origem = 0; origem < grafoDeClasses.quantidadeVertices(); origem++) {
            int pacoteOrigem = grafo.indiceDe(pacoteDe(grafoDeClasses.nome(origem)));
            for (int destino : grafoDeClasses.sucessores(origem)) {
                int pacoteDestino = grafo.indiceDe(pacoteDe(grafoDeClasses.nome(destino)));
                grafo.adicionarAresta(pacoteOrigem, pacoteDestino, 1); // mesmo pacote: o Digrafo ignora o laço
            }
        }
        return grafo;
    }

    /** {@code com.loja.web.Controller} → {@code com.loja.web} */
    public static String pacoteDe(String classe) {
        int ultimoPonto = classe.lastIndexOf('.');
        return ultimoPonto < 0 ? PACOTE_PADRAO : classe.substring(0, ultimoPonto);
    }
}
