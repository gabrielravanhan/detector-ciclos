package br.unisagrado.ciclos.api;

import br.unisagrado.ciclos.analise.Metricas;

import java.util.List;

/**
 * Formatos JSON devolvidos pela API (o "contrato" com a interface).
 * Os vértices são identificados pelo nome (pacote, classe ou rótulo da lista de arestas).
 */
public final class ModelosJson {

    private ModelosJson() {
    }

    public record ResumoDoGrafo(
            String idDoGrafo,
            String nome,
            String origem,
            List<ResumoDoNivel> niveis
    ) {
    }

    public record ResumoDoNivel(
            String nivel,
            int vertices,
            int arestas
    ) {
    }

    /** @param grupo pacote da classe (só no nível de classes); usado para agrupar no desenho */
    public record VerticeJson(
            String id,
            String rotulo,
            String grupo
    ) {
    }

    public record ArestaJson(
            String origem,
            String destino,
            int peso
    ) {
    }

    public record GrafoJson(
            String nivel,
            List<VerticeJson> vertices,
            List<ArestaJson> arestas
    ) {
    }

    /** @param id índice da CFC = iteração do Malgrange em que ela foi encontrada */
    public record ComponenteJson(
            int id,
            int tamanho,
            List<String> vertices
    ) {
    }

    /**
     * Uma iteração do Malgrange.
     *
     * @param niveisSucessores   níveis de Γ⁺(pivô): [[pivô], Γ¹, novos de Γ², ...]
     * @param niveisAntecessores níveis de Γ⁻(pivô): [[pivô], Γ⁻¹, novos de Γ⁻², ...]
     * @param componente         Γ⁺ ∩ Γ⁻ (CFC encontrada nesta iteração)
     */
    public record PassoJson(
            int iteracao,
            String pivo,
            List<List<String>> niveisSucessores,
            List<List<String>> niveisAntecessores,
            List<String> componente
    ) {
    }

    /** @param observacaoPassos explica por que os passos não vieram, quando for o caso */
    public record ResultadoMalgrangeJson(
            String nivel,
            Metricas metricas,
            List<ComponenteJson> componentes,
            List<PassoJson> passos,
            String observacaoPassos
    ) {
    }

    public record VerticeCondensadoJson(
            String id,
            int componente,
            int tamanho,
            int camada,
            String rotulo
    ) {
    }

    /** @param ordemTopologica ids das CFCs; se A → B, A aparece antes de B */
    public record CondensacaoJson(
            String nivel,
            List<VerticeCondensadoJson> vertices,
            List<ArestaJson> arestas,
            List<String> ordemTopologica
    ) {
    }

    /** @param dependenciasDeClasses pares "ClasseA -> ClasseB" que justificam uma aresta entre pacotes */
    public record EvidenciaJson(
            String origem,
            String destino,
            int peso,
            List<String> dependenciasDeClasses
    ) {
    }

    /** @param ciclo [v0, v1, ..., v0]; vazio se a CFC é trivial (sem ciclo) */
    public record CicloJson(
            String nivel,
            int componente,
            List<String> ciclo,
            List<EvidenciaJson> arestas
    ) {
    }
}
