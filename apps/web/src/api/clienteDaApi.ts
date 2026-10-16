import { CabecalhosBrewApp, type paths } from '@brewapp/shared';
import createClient, { type Middleware } from 'openapi-fetch';
import { ErroDaApi } from './ErroDaApi';

export interface OpcoesDoClienteDaApi {
  /** Origem do backend, sem o /api/v1, que já faz parte dos caminhos do contrato */
  urlBase: string;
  /** Token de acesso do Keycloak, guardado em memória na web (spec 9.5); null antes do login */
  obterToken?: () => string | null | Promise<string | null>;
  /** Organização em uso, enviada em X-Organization-Id; null antes de escolher uma */
  obterOrganizacaoId?: () => string | null;
  /** Substitui o fetch do navegador nos testes */
  fetch?: (requisicao: Request) => Promise<Response>;
}

/**
 * Cliente HTTP tipado pelo contrato OpenAPI (ADRs 0013 e 0017): caminho, parâmetros e corpos são conferidos pelo compilador.
 * Toda resposta de erro e toda falha de rede rejeitam a promessa com ErroDaApi, então o retorno de sucesso sempre
 * traz os dados. O tipo dos caminhos só muda nos testes, que usam um contrato próprio.
 */
export function criarClienteDaApi<Caminhos extends object = paths>(opcoes: OpcoesDoClienteDaApi) {
  const cliente = createClient<Caminhos>({ baseUrl: opcoes.urlBase, fetch: opcoes.fetch });
  cliente.use(criarMiddlewareDaApi(opcoes));
  return cliente;
}

function criarMiddlewareDaApi(opcoes: OpcoesDoClienteDaApi): Middleware {
  return {
    async onRequest({ request }) {
      const token = (await opcoes.obterToken?.()) ?? null;
      if (token !== null) {
        request.headers.set('Authorization', `Bearer ${token}`);
      }
      const organizacaoId = opcoes.obterOrganizacaoId?.() ?? null;
      if (organizacaoId !== null) {
        request.headers.set(CabecalhosBrewApp.X_ORGANIZATION_ID, organizacaoId);
      }
      return request;
    },
    async onResponse({ response }) {
      if (!response.ok) {
        throw await ErroDaApi.daResposta(response);
      }
      return response;
    },
    onError({ error }) {
      // Cancelamento pedido pela própria tela (AbortController) não é falha de conexão: segue como veio
      if (error instanceof DOMException && error.name === 'AbortError') {
        return;
      }
      return ErroDaApi.semConexao(error);
    },
  };
}
