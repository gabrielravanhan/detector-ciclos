# Detector de Ciclos de Dependência

Aplicação web que encontra **ciclos de dependência entre pacotes e classes de projetos Java**
usando o **algoritmo de Malgrange** para calcular as componentes fortemente conexas (CFC) do
grafo de dependências.

Trabalho da disciplina de Teoria dos Grafos — Tema 4 (Algoritmo de Malgrange).

## Integrantes

| Nome | Responsabilidade |
|---|---|
| _(preencher)_ | Backend Java: extração, grafo, Malgrange, API |
| _(preencher)_ | Frontend: visualização do grafo e das componentes |
| _(preencher)_ | Frontend: passo a passo do Malgrange (Γ⁺ / Γ⁻) e métricas |
| _(preencher)_ | Validação independente, gerador de grafos, medições de tempo |
| _(preencher)_ | Instâncias de teste, comparação entre versões, relatório |

## Requisitos

- **JDK 25** (precisa ser JDK, não JRE: a extração usa o `jdeps` que vem no JDK)
- **Maven 3.6.3 ou superior**

## Como compilar e executar

```bash
mvn package
java -jar target/detector-ciclos.jar
```

Abra <http://localhost:7070>. Opções: `--porta=8080` e `--instancias=caminho/da/pasta`
(os valores padrão ficam em `src/main/resources/application.yaml`).

`mvn package` também roda os testes automatizados. A interface funciona **sem internet**:
o Cytoscape.js vai empacotado dentro do jar.

## Funcionalidades

- Carregar uma instância pronta da pasta `instancias/`, enviar qualquer `.jar`/`.war` ou digitar
  uma lista de arestas
- Jars executáveis do Spring Boot: só o código do projeto (`BOOT-INF/classes`) é analisado;
  o lançador do Spring Boot e as bibliotecas embutidas ficam de fora (o mesmo vale para
  `WEB-INF/classes` num `.war`)
- Alternar entre o grafo de **pacotes** e o de **classes** (no de classes, cada pacote é uma caixa)
- Executar o Malgrange e ver as CFCs coloridas no grafo; opção para mostrar só os vértices em ciclos
- Passar o mouse sobre um vértice mostra o nome; clicar destaca seus vizinhos diretos
- **Passo a passo**: para cada iteração, o pivô, os níveis de Γ⁺ e Γ⁻ e a interseção
- **Ciclo concreto** de cada CFC (o menor ciclo, por busca em largura) e as dependências entre
  classes que justificam cada aresta entre pacotes
- **Grafo condensado** (DAG das CFCs) desenhado em camadas
- Métricas: número de CFCs, maior CFC, % de vértices em ciclos, tempo de execução

## Modelagem do grafo

| | Grafo de pacotes | Grafo de classes |
|---|---|---|
| Vértice | pacote Java | classe de nível superior |
| Aresta | P → Q se alguma classe de P usa alguma de Q | A → B se A referencia B |
| Direcionado | sim | sim |
| Peso | nº de pares de classes que geram a aresta | nº de referências |

- O peso **não é usado** pelo Malgrange; serve para indicar quais dependências são mais fracas
  e, portanto, mais baratas de remover para quebrar um ciclo.
- Laços (A → A) são ignorados.
- Classes internas e anônimas (`Foo$Bar`, `Foo$1`) são agrupadas na classe de nível superior.
- Apenas dependências internas ao projeto analisado são consideradas (`java.*` e bibliotecas
  de terceiros são descartadas).

## Algoritmo de Malgrange

Implementação em
[`Malgrange.java`](src/main/java/br/unisagrado/ciclos/algoritmo/Malgrange.java). Para cada
vértice pivô ainda não classificado, calcula o fecho transitivo direto Γ⁺ e o inverso Γ⁻, nível
a nível. A CFC do pivô é Γ⁺ ∩ Γ⁻. Esses vértices são marcados como removidos e o processo se
repete até não sobrar vértice. Complexidade O(V·(V+E)) no pior caso.

## Estrutura do código

O núcleo (grafo e algoritmos) não depende do Spring; a parte web segue o padrão
Controller → Service → Repository.

```
src/main/java/br/unisagrado/ciclos/
  Application.java   ponto de entrada (Spring Boot)
  grafo/             Digrafo: grafo direcionado ponderado (implementação própria); lista de arestas
  algoritmo/         Malgrange (+ registro dos passos)
  analise/           grafo condensado, menor ciclo, métricas
  extracao/          jdeps → dependências entre classes → grafos de classes e de pacotes
  projeto/           ProjetoController, ProjetoService, ProjetoRepository (em memória), dto/response
  instancia/         InstanciaController, InstanciaService (instâncias prontas), dto/response
  common/exception/  NaoEncontradoException, ErroResponse, GlobalExceptionHandler
src/main/resources/
  application.yaml   porta e pasta de instâncias
  public/            interface web (HTML, CSS e JavaScript puros)
instancias/          instâncias de teste
docs/exemplos-api/   exemplos reais das respostas da API
```

## API

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/instancias` | instâncias prontas |
| POST | `/api/projetos/instancia/{nome}` | carrega uma instância pronta |
| POST | `/api/projetos/envio?arquivo=x.jar` | carrega um .jar/.war (corpo = bytes do arquivo) |
| POST | `/api/projetos/arestas?nome=...` | carrega uma lista de arestas (corpo = texto) |
| GET | `/api/projetos/{id}/grafo?nivel=` | vértices e arestas |
| GET | `/api/projetos/{id}/exportar?nivel=` | o grafo como lista de arestas (texto) |
| POST | `/api/projetos/{id}/malgrange?nivel=&passos=true` | executa o Malgrange |
| GET | `/api/projetos/{id}/condensacao?nivel=` | grafo condensado |
| GET | `/api/projetos/{id}/componentes/{c}/ciclo?nivel=` | menor ciclo da CFC `c` |

`nivel` é `pacotes`, `classes` ou `informado` (lista de arestas); se omitido, usa o primeiro
nível do projeto. Os passos (`passos=true`) só são registrados para grafos de até 500 vértices.
Erros voltam como JSON com `status`, `erro`, `mensagem` e `path`.

Exemplos reais de resposta em [`docs/exemplos-api`](docs/exemplos-api) (exemplo de sala e Gson).

## Instâncias de teste

| Arquivo | Origem | Característica |
|---|---|---|
| `01-exemplo-sala.txt` | construído pelo grupo | 9 vértices, resultado conferível à mão |
| `02-camadas-sem-ciclos.txt` | construído pelo grupo | DAG: nenhum ciclo |
| `03-ciclo-total.txt` | construído pelo grupo | fronteira: uma única CFC com todos os vértices |
| `gson-2.10.1.jar` | Maven Central (Apache 2.0) | pequeno |
| `commons-lang3-3.12.0.jar`, `3.20.0.jar` | Maven Central (Apache 2.0) | médio; comparação entre versões |
| `spring-core-6.1.12.jar` | Maven Central (Apache 2.0) | médio |
| `tomcat-embed-core-10.1.28.jar` | Maven Central (Apache 2.0) | grande; jar modular |
| `hibernate-core-6.5.2.Final.jar` | Maven Central (LGPL 2.1) | muito grande: 4.892 classes |

Os jars podem ser baixados novamente em <https://central.sonatype.com> pelo nome e versão.

Formato da lista de arestas:

```
# comentário
A -> B          aresta de peso 1
A -> C 3        aresta de peso 3
D               vértice isolado
```

### Resultados

| Instância | Nível | Vértices | Arestas | CFCs | CFCs com ciclo | Maior CFC | Vértices em ciclos |
|---|---|---:|---:|---:|---:|---:|---:|
| exemplo de sala | informado | 9 | 10 | 4 | 3 | 3 | 88,9% |
| camadas sem ciclos | informado | 5 | 9 | 5 | 0 | 1 | 0% |
| ciclo total | informado | 6 | 8 | 1 | 1 | 6 | 100% |
| gson 2.10.1 | pacotes | 9 | 27 | 3 | 1 | 7 | 77,8% |
| gson 2.10.1 | classes | 75 | 360 | 35 | 3 | 39 | 57,3% |
| commons-lang3 3.12.0 | pacotes | 17 | 34 | 7 | 2 | 10 | 70,6% |
| commons-lang3 3.12.0 | classes | 198 | 340 | 156 | 5 | 37 | 23,7% |
| commons-lang3 3.20.0 | pacotes | 18 | 51 | 6 | 1 | 13 | 72,2% |
| commons-lang3 3.20.0 | classes | 241 | 472 | 191 | 5 | 45 | 22,8% |
| spring-core 6.1.12 | pacotes | 59 | 163 | 51 | 2 | 8 | 16,9% |
| spring-core 6.1.12 | classes | 724 | 3.027 | 626 | 21 | 27 | 16,4% |
| tomcat-embed-core 10.1.28 | pacotes | 78 | 555 | 48 | 8 | 18 | 48,7% |
| tomcat-embed-core 10.1.28 | classes | 971 | 5.541 | 717 | 35 | 140 | 29,8% |
| hibernate-core 6.5.2 | pacotes | 344 | 5.794 | 20 | 2 | 323 | 94,8% |
| hibernate-core 6.5.2 | classes | 4.892 | 48.329 | 1.476 | 16 | 3.387 | 70,2% |

O Malgrange leva menos de 1 ms nas instâncias pequenas e algumas dezenas de milissegundos no
grafo de classes do Hibernate (o tempo exato aparece nas métricas de cada execução).

## Bibliotecas utilizadas

Nenhuma biblioteca de grafos é usada: o grafo, o Malgrange, o grafo condensado e a busca de
ciclos foram implementados pelo grupo.

| Biblioteca | Uso |
|---|---|
| `jdeps` (parte do JDK) | **apenas extrair** as dependências entre classes do bytecode |
| Spring Boot 4.1 (Spring Web, Tomcat, Jackson) | servidor HTTP, rotas REST e conversão para JSON |
| Cytoscape.js 3.30 | **apenas desenho** do grafo na interface |
| cytoscape-fcose 2.2 | **apenas posicionamento** dos vértices no desenho (layout) |
| JUnit 5 e MockMvc (spring-boot-starter-test) | testes automatizados |

## Testes

`mvn test` executa os testes. Entre eles:

- exemplo de sala com resultado conhecido, incluindo os níveis de Γ⁺ e Γ⁻
- DAG (sem ciclos), ciclo único, grafo vazio, laço
- 300 grafos aleatórios comparados com a definição de CFC (alcance mútuo calculado por força bruta)
- grafo condensado sempre acíclico
- rotas da API testadas com MockMvc (sem abrir porta de rede)
- a ferramenta analisa o próprio código compilado e verifica que não há ciclos entre os pacotes
  ([`ArquiteturaTest`](src/test/java/br/unisagrado/ciclos/ArquiteturaTest.java))

## Limitações conhecidas

- Dependências por reflexão, injeção de dependência configurada externamente ou carregamento
  dinâmico não aparecem no bytecode.
- Constantes `static final` são copiadas pelo compilador para quem as usa, então essa
  dependência some.
- Os passos do Malgrange só são registrados para até 500 vértices; o desenho do grafo, para até
  2.500.
- Os projetos carregados ficam só em memória: somem quando a aplicação é fechada.

## Uso de Inteligência Artificial

_(preencher conforme a seção 7 do enunciado: ferramentas, etapas, conteúdos gerados e como
foram revisados)_
