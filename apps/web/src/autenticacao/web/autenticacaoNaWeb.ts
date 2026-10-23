import i18next from 'i18next';
import { OidcClient, WebStorageStateStore } from 'oidc-client-ts';
import { criarRenovacao, tokensDaResposta } from '../clienteOidc';
import type { ConfiguracaoOidc } from '../configuracaoOidc';
import { GerenciadorDeSessao } from '../GerenciadorDeSessao';
import type { AutenticacaoDaPlataforma } from '../plataforma';
import type { CofreDaSessao, TokensDaSessao } from '../sessao';

export const CAMINHO_DO_RETORNO_DO_LOGIN = '/auth/callback';

/** Na web nada sobrevive à página: o token fica só em memória (spec 9.5), e recarregar pede um novo login. */
const COFRE_SEM_PERSISTENCIA: CofreDaSessao = {
  ler: () => Promise.resolve(null),
  guardar: () => Promise.resolve(),
  apagar: () => Promise.resolve(),
};

/** O que vem do navegador, separado para os testes trocarem por fakes */
export interface RecursosDoNavegador {
  /** Origem do app, cadastrada no client brewapp-web do Keycloak (redirect e retorno do logout) */
  origem: string;
  /** Sai do app para o Keycloak; na web, a promessa de quem chamou fica pendente até a página ser trocada */
  navegarPara: (url: string) => void;
  /** Caminho e query da página atual, para onde o usuário volta depois do login */
  caminhoAtual: () => string;
  /** Guarda o state e o code_verifier do PKCE só durante o redirect; nunca token */
  armazenamentoDoLogin: Storage;
}

function recursosDoNavegador(): RecursosDoNavegador {
  return {
    origem: window.location.origin,
    navegarPara: (url) => {
      window.location.assign(url);
    },
    caminhoAtual: () => window.location.pathname + window.location.search,
    armazenamentoDoLogin: window.sessionStorage,
  };
}

/**
 * Login na web por redirect ao Keycloak (Authorization Code + PKCE). O state e o code_verifier precisam sobreviver à
 * ida e volta da página, por isso ficam no sessionStorage até o retorno; os tokens, nunca: só na memória.
 */
export function criarAutenticacaoNaWeb(
  configuracao: ConfiguracaoOidc,
  recursos: RecursosDoNavegador = recursosDoNavegador(),
  agora: () => number = Date.now,
): AutenticacaoDaPlataforma {
  const oidcClient = new OidcClient({
    authority: configuracao.emissor,
    client_id: configuracao.clientIdWeb,
    redirect_uri: `${recursos.origem}${CAMINHO_DO_RETORNO_DO_LOGIN}`,
    stateStore: new WebStorageStateStore({ store: recursos.armazenamentoDoLogin }),
  });
  const gerenciadorDeSessao = new GerenciadorDeSessao({
    renovar: criarRenovacao(oidcClient, agora),
    encerrarNoProvedor: (tokens) => encerrarNoKeycloak(oidcClient, recursos, tokens),
    cofre: COFRE_SEM_PERSISTENCIA,
    agora,
  });
  // O retorno é processado uma vez só: o state é consumido na primeira leitura, e o StrictMode repete os efeitos
  const retornosProcessados = new Map<string, Promise<string>>();

  return {
    gerenciadorDeSessao,
    abreNavegadorExterno: false,
    entrar: async () => {
      const pedidoDeLogin = await oidcClient.createSigninRequest({
        ui_locales: i18next.language,
        state: recursos.caminhoAtual(),
      });
      recursos.navegarPara(pedidoDeLogin.url);
    },
    concluirEntradaPorRedirect: (urlDeRetorno) => {
      let retorno = retornosProcessados.get(urlDeRetorno);
      if (retorno === undefined) {
        retorno = concluirEntrada(oidcClient, gerenciadorDeSessao, urlDeRetorno, agora);
        retornosProcessados.set(urlDeRetorno, retorno);
      }
      return retorno;
    },
  };
}

async function concluirEntrada(
  oidcClient: OidcClient,
  gerenciadorDeSessao: GerenciadorDeSessao,
  urlDeRetorno: string,
  agora: () => number,
): Promise<string> {
  const resposta = await oidcClient.processSigninResponse(urlDeRetorno);
  await gerenciadorDeSessao.registrarEntrada(tokensDaResposta(resposta, agora));
  return caminhoInterno(resposta.userState);
}

/** Só caminhos do próprio app: nada de "//outro-site" vindo de um state adulterado */
function caminhoInterno(destino: unknown): string {
  return typeof destino === 'string' && destino.startsWith('/') && !destino.startsWith('//') ? destino : '/';
}

/**
 * Logout pelo navegador (RP-Initiated Logout): o Keycloak encerra a sessão e volta para a página inicial. A URL é
 * montada aqui, sem o createSignoutRequest, que guardaria um state de logout que ninguém confere.
 */
async function encerrarNoKeycloak(
  oidcClient: OidcClient,
  recursos: RecursosDoNavegador,
  tokens: TokensDaSessao,
): Promise<void> {
  const endpointDeLogout = await oidcClient.metadataService.getEndSessionEndpoint();
  if (endpointDeLogout === undefined) {
    throw new Error('O provedor não publica o end_session_endpoint');
  }
  const urlDeLogout = new URL(endpointDeLogout);
  urlDeLogout.searchParams.set('client_id', oidcClient.settings.client_id);
  urlDeLogout.searchParams.set('id_token_hint', tokens.idToken);
  urlDeLogout.searchParams.set('post_logout_redirect_uri', `${recursos.origem}/`);
  recursos.navegarPara(urlDeLogout.toString());
}
