'use strict';

// Interface do Detector de Ciclos. Conversa com a API REST (ver ServidorApi.java) e usa o
// Cytoscape.js apenas para desenhar; todo o cálculo (Malgrange, condensação, ciclos) é feito no Java.

const NOMES_DOS_NIVEIS = { pacotes: 'Pacotes', classes: 'Classes', informado: 'Grafo informado' };
const PALETA = ['#e6194b', '#3cb44b', '#4363d8', '#f58231', '#911eb4', '#42d4f4',
                '#f032e6', '#9a6324', '#808000', '#469990', '#000075', '#800000'];
const COR_TRIVIAL = '#b8bcc4';
const COR_ARESTA = '#c9ced6';
const COR_CICLO = '#c62828';
const LIMITE_DE_VERTICES_DESENHADOS = 2500;
const LIMITE_DE_PASSOS_MOSTRADOS = 300;
const PREFIXO_DA_CAIXA_DE_PACOTE = 'pacote:';

const estado = {
    idDoGrafo: null,
    nivel: null,
    grafo: null,               // { nivel, vertices, arestas }
    resultado: null,           // resposta do Malgrange
    condensacao: null,
    corDaComponente: new Map(),       // id da CFC → cor
    componenteDoVertice: new Map(),   // nome do vértice → id da CFC
};

let desenho = null;            // Cytoscape do grafo
let desenhoCondensado = null;  // Cytoscape do grafo condensado
let retiradosPeloFiltro = null; // elementos tirados do desenho pelo filtro "só vértices em ciclos"

const $ = (id) => document.getElementById(id);

// ---------- utilidades ----------

async function chamarApi(metodo, url, corpo) {
    const resposta = await fetch(url, { method: metodo, body: corpo });
    const ehJson = (resposta.headers.get('content-type') || '').includes('json');
    const dados = ehJson ? await resposta.json() : await resposta.text();
    if (!resposta.ok) {
        throw new Error((dados && dados.erro) || dados || resposta.statusText);
    }
    return dados;
}

/** Executa uma ação da interface mostrando os erros na faixa de mensagens. */
async function protegido(acao) {
    mostrarMensagem('');
    document.body.style.cursor = 'progress';
    try {
        await acao();
    } catch (erro) {
        mostrarMensagem(erro.message, true);
    } finally {
        document.body.style.cursor = '';
    }
}

function mostrarMensagem(texto, ehErro = false) {
    const caixa = $('mensagem');
    caixa.textContent = texto;
    caixa.className = 'mensagem' + (ehErro ? ' erro' : '');
    caixa.hidden = !texto;
}

/** Cria um elemento: criar('div', { className: 'x' }, 'texto', outroElemento) */
function criar(tag, propriedades = {}, ...filhos) {
    const elemento = Object.assign(document.createElement(tag), propriedades);
    elemento.append(...filhos);
    return elemento;
}

function formatarTamanho(bytes) {
    return bytes > 1e6 ? (bytes / 1e6).toFixed(1) + ' MB' : Math.ceil(bytes / 1e3) + ' KB';
}

function resumirLista(nomes, maximo = 8) {
    const mostrados = nomes.slice(0, maximo).join(', ');
    return nomes.length > maximo ? `${mostrados} … (+${nomes.length - maximo})` : mostrados;
}

// ---------- carregamento ----------

async function carregarListaDeInstancias() {
    const instancias = await chamarApi('GET', '/api/instancias');
    const lista = $('lista-instancias');
    lista.replaceChildren(...instancias.map(i =>
        new Option(`${i.nome} (${i.tipo}, ${formatarTamanho(i.tamanhoEmBytes)})`, i.nome)));
    if (instancias.length === 0) {
        lista.append(new Option('(pasta instancias vazia)', ''));
    }
}

async function grafoCarregado(resumo) {
    estado.idDoGrafo = resumo.idDoGrafo;
    $('info-grafo').textContent = `${resumo.nome}: ` + resumo.niveis
        .map(n => `${NOMES_DOS_NIVEIS[n.nivel]} ${n.vertices} vértices / ${n.arestas} arestas`).join(' · ');

    const seletor = $('nivel');
    seletor.replaceChildren(...resumo.niveis.map(n => new Option(NOMES_DOS_NIVEIS[n.nivel], n.nivel)));
    seletor.disabled = false;
    $('executar').disabled = false;
    await mostrarGrafo(resumo.niveis[0].nivel);
}

async function mostrarGrafo(nivel) {
    estado.nivel = nivel;
    limparResultados();
    mostrarAba('grafo');
    estado.grafo = await chamarApi('GET', `/api/grafos/${estado.idDoGrafo}?nivel=${nivel}`);
    desenharGrafo();
}

function limparResultados() {
    estado.resultado = null;
    estado.condensacao = null;
    estado.corDaComponente.clear();
    estado.componenteDoVertice.clear();
    $('secao-metricas').hidden = true;
    $('componentes').replaceChildren('Execute o algoritmo.');
    $('ciclo').replaceChildren('Clique numa componente.');
    $('passos').replaceChildren();
    $('so-ciclos').checked = false;
    $('so-ciclos').disabled = true;
    if (desenhoCondensado) {
        desenhoCondensado.destroy();
        desenhoCondensado = null;
    }
}

// ---------- desenho do grafo ----------

function estiloBase() {
    return [
        { selector: 'node', style: {
            'background-color': COR_TRIVIAL, label: 'data(rotulo)', 'font-size': 11, color: '#1f2430',
            'text-valign': 'bottom', 'text-margin-y': 4, 'text-max-width': 180, 'text-wrap': 'ellipsis',
            'text-background-color': '#ffffff', 'text-background-opacity': 0.85,
            'text-background-padding': 2, 'text-background-shape': 'roundrectangle',
            // com pouco zoom os rótulos somem, em vez de virarem um borrão sobreposto
            'min-zoomed-font-size': 8 } },
        { selector: 'node[tamanho]', style: { width: 'data(tamanho)', height: 'data(tamanho)' } },
        { selector: 'node[cor]', style: { 'background-color': 'data(cor)' } },
        // caixa de um pacote (nível de classes)
        { selector: ':parent', style: {
            shape: 'round-rectangle', 'background-color': '#eef1f6', 'background-opacity': 0.7,
            'border-width': 1, 'border-color': COR_ARESTA, padding: 16,
            'text-valign': 'top', 'font-size': 12, 'font-weight': 'bold', color: '#4b5563',
            'text-background-opacity': 0, 'text-max-width': 400 } },
        { selector: 'edge', style: {
            width: 1, 'line-color': COR_ARESTA, 'target-arrow-color': COR_ARESTA,
            'target-arrow-shape': 'triangle', 'arrow-scale': 0.7, 'curve-style': 'bezier' } },
        { selector: 'edge[cor]', style: { width: 1.5, 'line-color': 'data(cor)', 'target-arrow-color': 'data(cor)' } },
        { selector: 'node.em-ciclo', style: { 'font-weight': 'bold', 'z-index': 5 } },
        { selector: '.esmaecido', style: { opacity: 0.08 } },
        { selector: 'node.foco', style: { 'border-width': 3, 'border-color': '#2f6fdb' } },
        { selector: 'edge.ciclo', style: {
            width: 3, 'line-color': COR_CICLO, 'target-arrow-color': COR_CICLO, 'z-index': 10 } },
        { selector: 'node.ciclo', style: { 'border-width': 3, 'border-color': COR_CICLO } },
    ];
}

/**
 * Layout por forças fCoSE: separa as caixas de pacotes sem sobreposição e leva em conta o
 * tamanho dos rótulos, para que vértices e textos não fiquem uns sobre os outros.
 */
function opcoesDeLayout(quantidadeDeVertices) {
    return {
        name: 'fcose',
        quality: quantidadeDeVertices <= 600 ? 'proof' : 'default',
        animate: false,
        randomize: true,
        padding: 30,
        nodeDimensionsIncludeLabels: true,
        packComponents: true,
        // valores ajustados medindo a sobreposição de rótulos e de caixas (Gson, nível de classes)
        nodeSeparation: 180,
        idealEdgeLength: () => 180,
        nodeRepulsion: () => 400000,
        edgeElasticity: () => 0.45,
        // mantém as classes de um pacote juntas dentro da caixa
        nestingFactor: 3,
        gravityCompound: 4,
        gravityRangeCompound: 0.8,
        gravity: 0.25,
        numIter: 2500,
    };
}

/**
 * Encurta os nomes de pacote removendo o prefixo comum a todos:
 * com.google.gson, com.google.gson.internal → gson, internal.
 */
function nomesCurtosDosPacotes(pacotes) {
    const partes = pacotes.map(p => p.split('.'));
    let comuns = 0;
    while (partes.length > 0
           && partes.every(p => p.length > comuns && p[comuns] === partes[0][comuns])) {
        comuns++;
    }
    return new Map(pacotes.map((pacote, i) => {
        const resto = partes[i].slice(comuns);
        // o pacote-raiz (igual ao prefixo comum) fica com o próprio último pedaço
        return [pacote, resto.length > 0 ? resto.join('.') : partes[i][partes[i].length - 1]];
    }));
}

function desenharGrafo() {
    if (desenho) {
        desenho.destroy();
        desenho = null;
    }
    retiradosPeloFiltro = null;
    const { nivel, vertices, arestas } = estado.grafo;
    if (vertices.length > LIMITE_DE_VERTICES_DESENHADOS) {
        mostrarMensagem(`O grafo tem ${vertices.length} vértices; o desenho foi omitido ` +
            `(limite ${LIMITE_DE_VERTICES_DESENHADOS}). Execute o algoritmo e use o grafo condensado ` +
            'e a lista de componentes.');
        return;
    }

    // vértices com mais ligações aparecem maiores
    const grau = new Map(vertices.map(v => [v.id, 0]));
    for (const a of arestas) {
        grau.set(a.origem, grau.get(a.origem) + 1);
        grau.set(a.destino, grau.get(a.destino) + 1);
    }
    // no nível de classes, cada pacote vira uma caixa com as suas classes dentro
    const porClasse = nivel === 'classes';
    const pacotes = porClasse
        ? [...new Set(vertices.map(v => v.grupo))]
        : nivel === 'pacotes' ? vertices.map(v => v.id) : [];
    const nomeCurto = nomesCurtosDosPacotes(pacotes);

    const elementos = [
        ...(porClasse
            ? pacotes.map(p => ({ data: { id: PREFIXO_DA_CAIXA_DE_PACOTE + p, rotulo: nomeCurto.get(p) } }))
            : []),
        ...vertices.map(v => ({ data: {
            id: v.id,
            rotulo: nivel === 'pacotes' ? nomeCurto.get(v.id) : v.rotulo,
            tamanho: 12 + 4 * Math.sqrt(grau.get(v.id)),
            ...(porClasse ? { parent: PREFIXO_DA_CAIXA_DE_PACOTE + v.grupo } : {}),
        } })),
        ...arestas.map((a, i) => ({ data: { id: 'a' + i, source: a.origem, target: a.destino, peso: a.peso } })),
    ];

    desenho = cytoscape({
        container: $('desenho-grafo'),
        elements: elementos,
        style: estiloBase(),
        layout: opcoesDeLayout(vertices.length),
        minZoom: 0.05,
        maxZoom: 3,
    });
    if (arestas.length > 1500) {
        desenho.edges().style('curve-style', 'straight'); // curvas custam caro em grafos densos
    }
    desenho.on('tap', evento => {
        if (evento.target === desenho) limparDestaque();
    });
    // ':childless' = vértices de verdade (as caixas de pacote têm filhos)
    desenho.on('tap', 'node:childless', evento => focarVertice(evento.target));
    desenho.on('mouseover', 'node:childless', evento => mostrarRotulos(evento.target));
    desenho.on('mouseout', 'node:childless', evento => {
        const vertice = evento.target;
        if (!vertice.hasClass('foco') && !vertice.hasClass('rotulado')) esconderRotulos(vertice);
    });
    desenho.on('zoom', () => mostrarRotulos(desenho.nodes('.rotulado')));
}

/**
 * Mostra o rótulo dos vértices num tamanho legível qualquer que seja o zoom (o rótulo normal
 * some quando o zoom deixa a fonte pequena demais).
 */
function mostrarRotulos(vertices) {
    const zoom = desenho.zoom();
    vertices.style({ 'font-size': 13 / zoom, 'min-zoomed-font-size': 0, 'text-max-width': 400 / zoom, 'z-index': 100 });
}

function esconderRotulos(vertices) {
    vertices.removeStyle('font-size min-zoomed-font-size text-max-width z-index');
}

/** Destaca o vértice clicado e seus vizinhos diretos (de quem ele depende e quem depende dele). */
function focarVertice(vertice) {
    const vizinhanca = vertice.closedNeighborhood();
    desenho.batch(() => {
        limparDestaque();
        desenho.nodes(':childless').not(vizinhanca).addClass('esmaecido');
        desenho.edges().not(vizinhanca).addClass('esmaecido');
        vertice.addClass('foco');
        vizinhanca.nodes().addClass('rotulado');
        mostrarRotulos(vizinhanca.nodes());
    });
    mostrarMensagem(`${vertice.id()}: depende de ${vertice.outgoers('node').length} e é usado por ` +
        `${vertice.incomers('node').length}. Clique no fundo para limpar.`);
}

/** Pinta cada CFC não trivial com uma cor; vértices fora de ciclos ficam cinza. */
function colorirGrafo() {
    if (!desenho) return;
    desenho.batch(() => {
        desenho.nodes(':childless').forEach(vertice => {
            const cor =estado.corDaComponente.get(estado.componenteDoVertice.get(vertice.id()));
            if (cor) vertice.data('cor', cor); else vertice.removeData('cor');
            vertice.toggleClass('em-ciclo', Boolean(cor));
        });
        desenho.edges().forEach(aresta => {
            const c = estado.componenteDoVertice.get(aresta.data('source'));
            const mesmaComponente = c === estado.componenteDoVertice.get(aresta.data('target'));
            const cor = mesmaComponente && estado.corDaComponente.get(c);
            if (cor) aresta.data('cor', cor); else aresta.removeData('cor');
        });
    });
}

/** Apaga o que está fora da CFC e marca em vermelho o ciclo encontrado. */
function destacar(componente, ciclo) {
    if (!desenho) return;
    const membros = new Set(componente.vertices);
    const noCiclo = new Set(ciclo.ciclo);
    const arestasDoCiclo = new Set();
    for (let i = 0; i + 1 < ciclo.ciclo.length; i++) {
        arestasDoCiclo.add(ciclo.ciclo[i] + '\u0000' + ciclo.ciclo[i + 1]);
    }
    desenho.batch(() => {
        esconderRotulos(desenho.nodes('.rotulado, .foco'));
        desenho.elements().removeClass('esmaecido ciclo foco rotulado');
        // só os vértices de verdade: esmaecer a caixa de um pacote esmaeceria as classes dentro dela
        desenho.nodes(':childless').forEach(vertice => {
            if (!membros.has(vertice.id())) vertice.addClass('esmaecido');
            if (noCiclo.has(vertice.id())) vertice.addClass('ciclo rotulado');
        });
        desenho.edges().forEach(aresta => {
            const origem = aresta.data('source');
            const destino = aresta.data('target');
            if (!membros.has(origem) || !membros.has(destino)) aresta.addClass('esmaecido');
            if (arestasDoCiclo.has(origem + '\u0000' + destino)) aresta.addClass('ciclo');
        });
    });
    desenho.fit(desenho.nodes(':childless').filter(vertice => membros.has(vertice.id())), 60);
    mostrarRotulos(desenho.nodes('.rotulado'));
}

function limparDestaque() {
    if (desenho) {
        esconderRotulos(desenho.nodes('.rotulado, .foco'));
        desenho.elements().removeClass('esmaecido ciclo foco rotulado');
    }
    document.querySelectorAll('.componente.selecionada').forEach(item => item.classList.remove('selecionada'));
}

/**
 * Mostra só os vértices que estão em alguma CFC não trivial e reorganiza o desenho.
 * Os demais saem do desenho (e voltam ao desmarcar), para o layout não reservar espaço para eles.
 */
function aplicarFiltroDeCiclos() {
    if (!desenho) return;
    if (retiradosPeloFiltro) {
        retiradosPeloFiltro.restore();
        retiradosPeloFiltro = null;
    }
    if ($('so-ciclos').checked) {
        const foraDeCiclos = desenho.nodes(':childless').not('.em-ciclo');
        const caixasVazias = desenho.nodes(':parent').filter(caixa => caixa.children().not(foraDeCiclos).empty());
        retiradosPeloFiltro = desenho.remove(foraDeCiclos.union(caixasVazias));
    }
    desenho.layout(opcoesDeLayout(desenho.nodes(':childless').length)).run();
}

// ---------- execução do Malgrange ----------

async function executarMalgrange() {
    const registrarPassos = $('registrar-passos').checked;
    const resultado = await chamarApi('POST',
        `/api/grafos/${estado.idDoGrafo}/malgrange?nivel=${estado.nivel}&passos=${registrarPassos}`);
    estado.resultado = resultado;

    definirCores(resultado.componentes);
    mostrarMetricas(resultado.metricas);
    mostrarComponentes(resultado.componentes);
    mostrarPassos(resultado);
    colorirGrafo();
    limparDestaque();
    $('so-ciclos').disabled = false;
    if ($('so-ciclos').checked) aplicarFiltroDeCiclos();

    estado.condensacao = await chamarApi('GET', `/api/grafos/${estado.idDoGrafo}/condensacao?nivel=${estado.nivel}`);
    if (desenhoCondensado) {
        desenhoCondensado.destroy();
        desenhoCondensado = null;
    }
    if ($('aba-condensado').classList.contains('ativa')) desenharCondensacao();
    if (resultado.observacaoPassos) mostrarMensagem(resultado.observacaoPassos);
}

/** CFCs com mais de um vértice (as que têm ciclo), da maior para a menor. */
function componentesComCiclo(componentes) {
    return componentes.filter(c => c.tamanho > 1).sort((a, b) => b.tamanho - a.tamanho);
}

function definirCores(componentes) {
    estado.corDaComponente.clear();
    estado.componenteDoVertice.clear();
    componentesComCiclo(componentes).forEach((c, posicao) =>
        estado.corDaComponente.set(c.id, PALETA[posicao % PALETA.length]));
    for (const c of componentes) {
        for (const vertice of c.vertices) estado.componenteDoVertice.set(vertice, c.id);
    }
}

function mostrarMetricas(m) {
    const linhas = [
        ['Vértices', m.vertices],
        ['Arestas', m.arestas],
        ['Componentes (CFC)', m.componentes],
        ['CFC com ciclo', m.componentesComCiclo],
        ['Maior CFC', m.maiorComponente],
        ['Vértices em ciclos', `${m.verticesEmCiclos} (${m.percentualEmCiclos.toFixed(1)}%)`],
        ['Tempo do Malgrange', `${m.tempoMilissegundos.toFixed(2)} ms`],
    ];
    $('metricas').replaceChildren(...linhas.flatMap(([rotulo, valor]) =>
        [criar('dt', {}, rotulo), criar('dd', {}, String(valor))]));
    $('secao-metricas').hidden = false;
}

function mostrarComponentes(componentes) {
    const comCiclo = componentesComCiclo(componentes);
    if (comCiclo.length === 0) {
        $('componentes').replaceChildren(
            `Nenhum ciclo: as ${componentes.length} componentes são triviais (o grafo é acíclico).`);
        return;
    }
    $('componentes').replaceChildren(...comCiclo.map(componente => {
        const item = criar('div', { className: 'componente' },
            criar('strong', {}, `CFC ${componente.id} · ${componente.tamanho} vértices`),
            criar('small', {}, resumirLista(componente.vertices, 6)));
        item.dataset.id = componente.id;
        item.style.setProperty('--cor', estado.corDaComponente.get(componente.id));
        item.addEventListener('click', () => protegido(() => selecionarComponente(componente)));
        return item;
    }));
}

async function selecionarComponente(componente) {
    document.querySelectorAll('.componente').forEach(item =>
        item.classList.toggle('selecionada', Number(item.dataset.id) === componente.id));
    const ciclo = await chamarApi('GET',
        `/api/grafos/${estado.idDoGrafo}/componentes/${componente.id}/ciclo?nivel=${estado.nivel}`);
    mostrarCiclo(ciclo);
    destacar(componente, ciclo);
}

/** Mostra o ciclo e, no nível de pacotes, quais classes criam cada aresta (onde quebrar o ciclo). */
function mostrarCiclo(ciclo) {
    const partes = [criar('div', { className: 'caminho-do-ciclo' }, ciclo.ciclo.join(' → '))];
    for (const aresta of ciclo.arestas) {
        const caixa = criar('div', { className: 'evidencia' },
            criar('strong', {}, `${aresta.origem} → ${aresta.destino}`), ` (peso ${aresta.peso})`);
        if (aresta.dependenciasDeClasses.length) {
            caixa.append(criar('ul', {}, ...aresta.dependenciasDeClasses.map(par => criar('li', {}, par))));
        }
        partes.push(caixa);
    }
    $('ciclo').replaceChildren(...partes);
}

// ---------- passo a passo ----------

function mostrarPassos(resultado) {
    const area = $('passos');
    if (resultado.passos.length === 0) {
        area.replaceChildren(criar('p', { className: 'apagado' }, resultado.observacaoPassos
            || 'Marque "Registrar passos" e execute novamente para ver Γ⁺ e Γ⁻ de cada iteração.'));
        return;
    }
    const mostrados = resultado.passos.slice(0, LIMITE_DE_PASSOS_MOSTRADOS).map(passo => {
        const linhas = [
            ...passo.niveisSucessores.map((nivel, k) => linha(`Γ⁺ nível ${k}`, nivel)),
            ...passo.niveisAntecessores.map((nivel, k) => linha(`Γ⁻ nível ${k}`, nivel)),
            linha('Γ⁺ ∩ Γ⁻ (CFC)', passo.componente),
        ];
        const aberto = passo.componente.length > 1 && resultado.passos.length <= 30;
        return criar('details', { className: 'passo', open: aberto },
            criar('summary', {}, `Iteração ${passo.iteracao}: pivô ${passo.pivo} → CFC {${resumirLista(passo.componente, 6)}}`),
            criar('table', {}, ...linhas));
    });
    if (resultado.passos.length > LIMITE_DE_PASSOS_MOSTRADOS) {
        mostrados.push(criar('p', { className: 'apagado' },
            `… mais ${resultado.passos.length - LIMITE_DE_PASSOS_MOSTRADOS} iterações.`));
    }
    area.replaceChildren(...mostrados);

    function linha(rotulo, nomes) {
        return criar('tr', {}, criar('td', {}, rotulo), criar('td', {}, nomes.join(', ')));
    }
}

// ---------- grafo condensado ----------

/** Desenha o DAG das CFCs em camadas (camada = maior caminho até a CFC), de cima para baixo. */
function desenharCondensacao() {
    if (desenhoCondensado || !estado.condensacao) return;
    const { vertices, arestas } = estado.condensacao;
    if (vertices.length > LIMITE_DE_VERTICES_DESENHADOS) {
        mostrarMensagem(`O grafo condensado tem ${vertices.length} vértices; o desenho foi omitido.`);
        return;
    }
    const ocupadosPorCamada = new Map();
    const elementos = vertices.map(v => {
        const posicaoNaCamada = ocupadosPorCamada.get(v.camada) || 0;
        ocupadosPorCamada.set(v.camada, posicaoNaCamada + 1);
        return {
            data: {
                id: v.id,
                rotulo: v.rotulo,
                cor: estado.corDaComponente.get(v.componente) || COR_TRIVIAL,
                tamanho: 14 + 6 * Math.sqrt(v.tamanho - 1),
            },
            position: { x: posicaoNaCamada * 110, y: v.camada * 110 },
        };
    });
    elementos.push(...arestas.map((a, i) => ({ data: { id: 'ca' + i, source: a.origem, target: a.destino } })));

    desenhoCondensado = cytoscape({
        container: $('desenho-condensado'),
        elements: elementos,
        style: estiloBase(),
        layout: { name: 'preset' },
    });
}

// ---------- abas e eventos ----------

function mostrarAba(nome) {
    document.querySelectorAll('.abas button').forEach(b => b.classList.toggle('ativa', b.dataset.aba === nome));
    document.querySelectorAll('.aba').forEach(aba => aba.classList.toggle('ativa', aba.id === 'aba-' + nome));
    if (nome === 'grafo' && desenho) desenho.resize();
    if (nome === 'condensado') desenharCondensacao();
}

document.querySelectorAll('.abas button').forEach(botao =>
    botao.addEventListener('click', () => mostrarAba(botao.dataset.aba)));

$('carregar-instancia').addEventListener('click', () => protegido(async () => {
    const nome = $('lista-instancias').value;
    if (!nome) throw new Error('Nenhuma instância selecionada.');
    await grafoCarregado(await chamarApi('POST', `/api/grafos/instancia/${encodeURIComponent(nome)}`));
}));

$('enviar-jar').addEventListener('click', () => protegido(async () => {
    const arquivo = $('arquivo-jar').files[0];
    if (!arquivo) throw new Error('Escolha um arquivo .jar ou .war.');
    await grafoCarregado(await chamarApi('POST',
        `/api/grafos/envio?arquivo=${encodeURIComponent(arquivo.name)}`, arquivo));
}));

$('carregar-arestas').addEventListener('click', () => protegido(async () => {
    await grafoCarregado(await chamarApi('POST', '/api/grafos/arestas?nome=grafo%20manual', $('texto-arestas').value));
}));

$('nivel').addEventListener('change', evento => protegido(() => mostrarGrafo(evento.target.value)));
$('executar').addEventListener('click', () => protegido(executarMalgrange));
$('so-ciclos').addEventListener('change', aplicarFiltroDeCiclos);

protegido(carregarListaDeInstancias);
