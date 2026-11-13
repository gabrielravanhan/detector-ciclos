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
            // com pouco zoom os rótulos somem, em vez de virarem um borrão sobreposto
            'min-zoomed-font-size': 8 } },
        { selector: 'node[tamanho]', style: { width: 'data(tamanho)', height: 'data(tamanho)' } },
        { selector: 'node[cor]', style: { 'background-color': 'data(cor)' } },
        { selector: 'edge', style: {
            width: 1, 'line-color': COR_ARESTA, 'target-arrow-color': COR_ARESTA,
            'target-arrow-shape': 'triangle', 'arrow-scale': 0.7, 'curve-style': 'bezier' } },
        { selector: 'edge[cor]', style: { width: 1.5, 'line-color': 'data(cor)', 'target-arrow-color': 'data(cor)' } },
        { selector: 'node.em-ciclo', style: { 'font-weight': 'bold', 'z-index': 5 } },
        { selector: '.esmaecido', style: { opacity: 0.08 } },
        { selector: 'edge.ciclo', style: {
            width: 3, 'line-color': COR_CICLO, 'target-arrow-color': COR_CICLO, 'z-index': 10 } },
        { selector: 'node.ciclo', style: { 'border-width': 3, 'border-color': COR_CICLO } },
    ];
}

function desenharGrafo() {
    if (desenho) {
        desenho.destroy();
        desenho = null;
    }
    const { vertices, arestas } = estado.grafo;
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
    const elementos = [
        ...vertices.map(v => ({ data: { id: v.id, rotulo: v.rotulo, tamanho: 12 + 4 * Math.sqrt(grau.get(v.id)) } })),
        ...arestas.map((a, i) => ({ data: { id: 'a' + i, source: a.origem, target: a.destino, peso: a.peso } })),
    ];

    desenho = cytoscape({
        container: $('desenho-grafo'),
        elements: elementos,
        style: estiloBase(),
        layout: { name: 'cose', animate: false, padding: 30 },
        minZoom: 0.05,
        maxZoom: 3,
    });
    desenho.on('tap', evento => {
        if (evento.target === desenho) limparDestaque();
    });
}

/** Pinta cada CFC não trivial com uma cor; vértices fora de ciclos ficam cinza. */
function colorirGrafo() {
    if (!desenho) return;
    desenho.batch(() => {
        desenho.nodes().forEach(vertice => {
            const cor = estado.corDaComponente.get(estado.componenteDoVertice.get(vertice.id()));
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
        desenho.elements().removeClass('esmaecido ciclo');
        desenho.nodes().forEach(vertice => {
            if (!membros.has(vertice.id())) vertice.addClass('esmaecido');
            if (noCiclo.has(vertice.id())) vertice.addClass('ciclo');
        });
        desenho.edges().forEach(aresta => {
            const origem = aresta.data('source');
            const destino = aresta.data('target');
            if (!membros.has(origem) || !membros.has(destino)) aresta.addClass('esmaecido');
            if (arestasDoCiclo.has(origem + '\u0000' + destino)) aresta.addClass('ciclo');
        });
    });
    desenho.fit(desenho.nodes().filter(vertice => membros.has(vertice.id())), 60);
}

function limparDestaque() {
    if (desenho) desenho.elements().removeClass('esmaecido ciclo');
    document.querySelectorAll('.componente.selecionada').forEach(item => item.classList.remove('selecionada'));
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

protegido(carregarListaDeInstancias);
