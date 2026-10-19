// @vitest-environment node
// Só fetch, Request e Response: o ambiente do Node basta e evita as diferenças de AbortSignal do jsdom
import { describe, expect, it } from 'vitest';
import { criarClienteDaApi, type OpcoesDoClienteDaApi } from './clienteDaApi';
import { CodigoDeErroDoCliente, ErroDaApi } from './ErroDaApi';

// Contrato mínimo no formato gerado pelo openapi-typescript: o contrato real ainda não tem endpoints
interface CaminhosDeTeste {
  '/api/v1/recipientes': {
    parameters: { query?: never; header?: never; path?: never; cookie?: never };
    get: {
      parameters: { query?: never; header?: never; path?: never; cookie?: never };
      requestBody?: never;
      responses: {
        200: { headers: Record<string, unknown>; content: { 'application/json': { nome: string } } };
      };
    };
  };
}

const URL_BASE = 'http://localhost:8080';

interface FetchFalso {
  fetch: (requisicao: Request) => Promise<Response>;
  requisicoesFeitas: Request[];
}

function responderCom(resposta: () => Response | Promise<Response>): FetchFalso {
  const requisicoesFeitas: Request[] = [];
  return {
    requisicoesFeitas,
    fetch: async (requisicao) => {
      requisicoesFeitas.push(requisicao);
      return resposta();
    },
  };
}

function criarCliente(fetchFalso: FetchFalso, opcoes: Partial<OpcoesDoClienteDaApi> = {}) {
  return criarClienteDaApi<CaminhosDeTeste>({ urlBase: URL_BASE, fetch: fetchFalso.fetch, ...opcoes });
}

function respostaJson(corpo: unknown, status: number, tipo = 'application/json') {
  return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': tipo } });
}

async function capturarErro(promessa: Promise<unknown>): Promise<unknown> {
  try {
    await promessa;
  } catch (erro) {
    return erro;
  }
  throw new Error('A chamada deveria ter falhado');
}

async function capturarErroDaApi(promessa: Promise<unknown>): Promise<ErroDaApi> {
  const erro = await capturarErro(promessa);
  if (!(erro instanceof ErroDaApi)) {
    throw new Error(`Esperado ErroDaApi, veio ${String(erro)}`);
  }
  return erro;
}

describe('cliente da API', () => {
  it('devolve os dados da resposta de sucesso', async () => {
    const cliente = criarCliente(responderCom(() => respostaJson({ nome: 'Fermentador 1' }, 200)));

    const { data } = await cliente.GET('/api/v1/recipientes');

    expect(data).toEqual({ nome: 'Fermentador 1' });
  });

  it('envia o token e a organização em uso quando existem', async () => {
    const fetchFalso = responderCom(() => respostaJson({ nome: 'Fermentador 1' }, 200));
    const cliente = criarCliente(fetchFalso, {
      obterToken: () => Promise.resolve('token-de-teste'),
      obterOrganizacaoId: () => '0192f3a0-0000-7000-8000-000000000001',
    });

    await cliente.GET('/api/v1/recipientes');

    const cabecalhos = fetchFalso.requisicoesFeitas[0]?.headers;
    expect(cabecalhos?.get('Authorization')).toBe('Bearer token-de-teste');
    expect(cabecalhos?.get('X-Organization-Id')).toBe('0192f3a0-0000-7000-8000-000000000001');
  });

  it('não envia token nem organização antes do login e da escolha da organização', async () => {
    const fetchFalso = responderCom(() => respostaJson({ nome: 'Fermentador 1' }, 200));
    const cliente = criarCliente(fetchFalso, { obterToken: () => null, obterOrganizacaoId: () => null });

    await cliente.GET('/api/v1/recipientes');

    const cabecalhos = fetchFalso.requisicoesFeitas[0]?.headers;
    expect(cabecalhos?.has('Authorization')).toBe(false);
    expect(cabecalhos?.has('X-Organization-Id')).toBe(false);
  });

  it('rejeita com o code, os params e o correlationId do Problem Details', async () => {
    const naoEncontrado = {
      type: 'https://brewapp.dev/erros/recipiente-nao-encontrado',
      title: 'Recipiente não encontrado',
      status: 404,
      code: 'RECIPIENTE_NAO_ENCONTRADO',
      params: { recipienteId: '0192' },
      correlationId: 'correlacao-404',
      errors: [],
    };
    const cliente = criarCliente(responderCom(() => respostaJson(naoEncontrado, 404, 'application/problem+json')));

    const erro = await capturarErroDaApi(cliente.GET('/api/v1/recipientes'));

    expect(erro.code).toBe('RECIPIENTE_NAO_ENCONTRADO');
    expect(erro.status).toBe(404);
    expect(erro.params).toEqual({ recipienteId: '0192' });
    expect(erro.correlationId).toBe('correlacao-404');
    expect(erro.errors).toEqual([]);
  });

  it('rejeita falha de validação com os campos em errors', async () => {
    const validacaoFalhou = {
      type: 'https://brewapp.dev/erros/validacao-falhou',
      title: 'Validação falhou',
      status: 400,
      code: 'VALIDACAO_FALHOU',
      params: {},
      correlationId: 'correlacao-400',
      errors: [{ campo: 'capacidade.valor', code: 'DEVE_SER_POSITIVO', params: {} }],
    };
    const cliente = criarCliente(responderCom(() => respostaJson(validacaoFalhou, 400, 'application/problem+json')));

    const erro = await capturarErroDaApi(cliente.GET('/api/v1/recipientes'));

    expect(erro.code).toBe('VALIDACAO_FALHOU');
    expect(erro.errors).toEqual([{ campo: 'capacidade.valor', code: 'DEVE_SER_POSITIVO', params: {} }]);
  });

  it('rejeita resposta de erro fora do formato com RESPOSTA_INESPERADA, mantendo status e correlação', async () => {
    const paginaDoProxy = () =>
      new Response('<html>Bad Gateway</html>', {
        status: 502,
        headers: { 'Content-Type': 'text/html', 'X-Correlation-Id': 'correlacao-502' },
      });
    const cliente = criarCliente(responderCom(paginaDoProxy));

    const erro = await capturarErroDaApi(cliente.GET('/api/v1/recipientes'));

    expect(erro.code).toBe(CodigoDeErroDoCliente.RESPOSTA_INESPERADA);
    expect(erro.status).toBe(502);
    expect(erro.correlationId).toBe('correlacao-502');
  });

  it('rejeita falha de rede com SEM_CONEXAO, sem status e com a causa original', async () => {
    const falhaDeRede = new TypeError('fetch failed');
    const cliente = criarCliente(responderCom(() => Promise.reject(falhaDeRede)));

    const erro = await capturarErroDaApi(cliente.GET('/api/v1/recipientes'));

    expect(erro.code).toBe(CodigoDeErroDoCliente.SEM_CONEXAO);
    expect(erro.status).toBeNull();
    expect(erro.correlationId).toBeNull();
    expect(erro.cause).toBe(falhaDeRede);
  });

  it('deixa passar o cancelamento pedido pela tela, sem tratá-lo como falha de conexão', async () => {
    const cancelamento = new DOMException('A requisição foi cancelada', 'AbortError');
    const cliente = criarCliente(responderCom(() => Promise.reject(cancelamento)));

    const erro = await capturarErro(cliente.GET('/api/v1/recipientes'));

    expect(erro).toBe(cancelamento);
  });
});
