package br.unisagrado.ciclos.algoritmo;

import br.unisagrado.ciclos.grafo.Digrafo;
import br.unisagrado.ciclos.grafo.ListaDeArestas;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MalgrangeTest {

    /** Exemplo de sala. CFCs esperadas: {A, B, C}, {D, E, F}, {G, H} e {I}. */
    private static final String EXEMPLO_DE_SALA = """
            A -> B
            B -> C
            C -> A
            B -> D
            D -> E
            E -> F
            F -> D
            G -> F
            G -> H
            H -> G
            I
            """;

    @Test
    void exemploDeSalaTemQuatroComponentes() {
        Digrafo grafo = ListaDeArestas.ler(EXEMPLO_DE_SALA);
        ResultadoMalgrange resultado = Malgrange.executar(grafo, false);

        assertEquals(Set.of(Set.of("A", "B", "C"), Set.of("D", "E", "F"), Set.of("G", "H"), Set.of("I")),
                nomesDasComponentes(grafo, resultado));
    }

    @Test
    void registraOsNiveisDeSucessoresEAntecessores() {
        Digrafo grafo = ListaDeArestas.ler(EXEMPLO_DE_SALA);
        PassoMalgrange primeiro = Malgrange.executar(grafo, true).passos().get(0);

        // pivô A: Γ⁺ = {A} | {B} | {C, D} | {E} | {F}
        assertEquals("A", grafo.nome(primeiro.pivo()));
        assertEquals(List.of(List.of("A"), List.of("B"), List.of("C", "D"), List.of("E"), List.of("F")),
                nomesDosNiveis(grafo, primeiro.niveisSucessores()));
        // Γ⁻ = {A} | {C} | {B}
        assertEquals(List.of(List.of("A"), List.of("C"), List.of("B")),
                nomesDosNiveis(grafo, primeiro.niveisAntecessores()));
        assertEquals(Set.of("A", "B", "C"), nomes(grafo, primeiro.componente()));
    }

    @Test
    void segundaIteracaoIgnoraVerticesRemovidos() {
        Digrafo grafo = ListaDeArestas.ler(EXEMPLO_DE_SALA);
        PassoMalgrange segundo = Malgrange.executar(grafo, true).passos().get(1);

        // pivô D: A, B e C já foram removidos, então B não aparece em Γ⁻(D)
        assertEquals("D", grafo.nome(segundo.pivo()));
        assertEquals(List.of(List.of("D"), List.of("F"), List.of("E", "G"), List.of("H")),
                nomesDosNiveis(grafo, segundo.niveisAntecessores()));
        assertEquals(Set.of("D", "E", "F"), nomes(grafo, segundo.componente()));
    }

    @Test
    void grafoSemCiclosTemSoComponentesTriviais() {
        Digrafo grafo = ListaDeArestas.ler("a -> b\nb -> c\na -> c\nc -> d");
        ResultadoMalgrange resultado = Malgrange.executar(grafo, false);

        assertEquals(4, resultado.componentes().size());
        assertTrue(resultado.componentes().stream().allMatch(c -> c.size() == 1));
    }

    @Test
    void cicloUnicoEhUmaComponente() {
        Digrafo grafo = ListaDeArestas.ler("a -> b\nb -> c\nc -> d\nd -> a");
        ResultadoMalgrange resultado = Malgrange.executar(grafo, false);

        assertEquals(1, resultado.componentes().size());
        assertEquals(4, resultado.componentes().get(0).size());
    }

    @Test
    void grafoVazioNaoTemComponentes() {
        assertTrue(Malgrange.executar(new Digrafo(), true).componentes().isEmpty());
    }

    @Test
    void lacoNaoFormaCiclo() {
        Digrafo grafo = ListaDeArestas.ler("a -> a\na -> b");
        ResultadoMalgrange resultado = Malgrange.executar(grafo, false);

        assertEquals(2, resultado.componentes().size());
    }

    @Test
    void semRegistroDePassosAListaDePassosFicaVazia() {
        Digrafo grafo = ListaDeArestas.ler(EXEMPLO_DE_SALA);

        assertTrue(Malgrange.executar(grafo, false).passos().isEmpty());
        assertEquals(4, Malgrange.executar(grafo, true).passos().size(), "um passo por CFC encontrada");
    }

    /**
     * Compara com a definição de CFC em grafos aleatórios: u e v estão na mesma componente
     * se e somente se u alcança v e v alcança u (alcance calculado por força bruta).
     */
    @Test
    void concordaComAlcanceMutuoEmGrafosAleatorios() {
        Random aleatorio = new Random(42);
        for (int tentativa = 0; tentativa < 300; tentativa++) {
            int n = 1 + aleatorio.nextInt(40);
            double densidade = aleatorio.nextDouble() * 0.15;
            Digrafo grafo = new Digrafo();
            for (int v = 0; v < n; v++) {
                grafo.adicionarVertice("v" + v);
            }
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (aleatorio.nextDouble() < densidade) {
                        grafo.adicionarAresta(u, v);
                    }
                }
            }

            ResultadoMalgrange resultado = Malgrange.executar(grafo, false);
            int[] componenteDe = componenteDeCadaVertice(resultado, n);
            assertEquals(n, resultado.componentes().stream().mapToInt(List::size).sum(),
                    "cada vértice deve estar em exatamente uma CFC");

            boolean[][] alcanca = alcancePorForcaBruta(grafo);
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    boolean mesmaComponente = componenteDe[u] == componenteDe[v];
                    assertEquals(alcanca[u][v] && alcanca[v][u], mesmaComponente,
                            "tentativa " + tentativa + ": vértices " + u + " e " + v);
                }
            }
        }
    }

    /** componenteDe[v] = índice da CFC que contém o vértice v. */
    private static int[] componenteDeCadaVertice(ResultadoMalgrange resultado, int quantidadeVertices) {
        int[] componenteDe = new int[quantidadeVertices];
        for (int c = 0; c < resultado.componentes().size(); c++) {
            for (int vertice : resultado.componentes().get(c)) {
                componenteDe[vertice] = c;
            }
        }
        return componenteDe;
    }

    /** alcanca[u][v] = existe caminho de u até v (busca em profundidade a partir de cada vértice). */
    private static boolean[][] alcancePorForcaBruta(Digrafo grafo) {
        int n = grafo.quantidadeVertices();
        boolean[][] alcanca = new boolean[n][n];
        for (int inicio = 0; inicio < n; inicio++) {
            Deque<Integer> pilha = new ArrayDeque<>();
            pilha.push(inicio);
            alcanca[inicio][inicio] = true;
            while (!pilha.isEmpty()) {
                int vertice = pilha.pop();
                for (int proximo : grafo.sucessores(vertice)) {
                    if (!alcanca[inicio][proximo]) {
                        alcanca[inicio][proximo] = true;
                        pilha.push(proximo);
                    }
                }
            }
        }
        return alcanca;
    }

    private static Set<Set<String>> nomesDasComponentes(Digrafo grafo, ResultadoMalgrange resultado) {
        Set<Set<String>> componentes = new HashSet<>();
        for (List<Integer> componente : resultado.componentes()) {
            componentes.add(nomes(grafo, componente));
        }
        return componentes;
    }

    private static Set<String> nomes(Digrafo grafo, List<Integer> vertices) {
        return vertices.stream().map(grafo::nome).collect(Collectors.toSet());
    }

    private static List<List<String>> nomesDosNiveis(Digrafo grafo, List<List<Integer>> niveis) {
        return niveis.stream().map(nivel -> nivel.stream().map(grafo::nome).toList()).toList();
    }
}
