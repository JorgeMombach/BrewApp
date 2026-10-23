import { openUrl } from '@tauri-apps/plugin-opener';
import i18next from 'i18next';
import { InMemoryWebStorage, OidcClient, WebStorageStateStore } from 'oidc-client-ts';
import { criarRenovacao, tokensDaResposta } from '../clienteOidc';
import type { ConfiguracaoOidc } from '../configuracaoOidc';
import { GerenciadorDeSessao } from '../GerenciadorDeSessao';
import type { AutenticacaoDaPlataforma } from '../plataforma';
import type { CofreDaSessao, TokensDaSessao } from '../sessao';
import { cofreDoDesktop, escutarRetornoDoLogin, type RetornoDoLogin } from './nativo';

/** O que o desktop precisa de nativo, separado para os testes trocarem por fakes */
export interface RecursosNativos {
  cofre: CofreDaSessao;
  escutarRetornoDoLogin: (paginaDeRetorno: string) => Promise<RetornoDoLogin>;
  abrirNoNavegador: (url: string) => Promise<void>;
}

const RECURSOS_DO_TAURI: RecursosNativos = {
  cofre: cofreDoDesktop,
  escutarRetornoDoLogin,
  abrirNoNavegador: openUrl,
};

/**
 * Login no desktop pelo navegador do sistema (RFC 8252): o app nunca vê a senha, e o usuário usa o gerenciador de
 * senhas do navegador. O retorno chega num servidor de loopback, numa porta livre a cada login.
 */
export function criarAutenticacaoNoDesktop(
  configuracao: ConfiguracaoOidc,
  recursosNativos: RecursosNativos = RECURSOS_DO_TAURI,
  agora: () => number = Date.now,
): AutenticacaoDaPlataforma {
  const oidcClient = criarOidcClientDoDesktop(configuracao);
  const gerenciadorDeSessao = new GerenciadorDeSessao({
    renovar: criarRenovacao(oidcClient, agora),
    encerrarNoProvedor: criarEncerramentoNoDesktop(oidcClient),
    cofre: recursosNativos.cofre,
    agora,
  });
  return {
    gerenciadorDeSessao,
    abreNavegadorExterno: true,
    entrar: async () => {
      const tokens = await entrarPeloNavegador(oidcClient, recursosNativos, agora);
      await gerenciadorDeSessao.registrarEntrada(tokens);
    },
  };
}

function criarOidcClientDoDesktop(configuracao: ConfiguracaoOidc): OidcClient {
  return new OidcClient({
    authority: configuracao.emissor,
    client_id: configuracao.clientIdDesktop,
    // Trocado a cada login pela porta do loopback (createSigninRequest); este só cumpre o campo obrigatório
    redirect_uri: 'http://127.0.0.1/auth/callback',
    // O state e o code_verifier do PKCE só vivem durante o login, na mesma página: memória basta
    stateStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  });
}

async function entrarPeloNavegador(
  oidcClient: OidcClient,
  recursosNativos: RecursosNativos,
  agora: () => number,
): Promise<TokensDaSessao> {
  const { porta, urlDeRetorno } = await recursosNativos.escutarRetornoDoLogin(paginaDeRetorno());
  const pedidoDeLogin = await oidcClient.createSigninRequest({
    redirect_uri: `http://127.0.0.1:${String(porta)}/auth/callback`,
    // O Keycloak abre o login no idioma em uso no app
    ui_locales: i18next.language,
  });
  await recursosNativos.abrirNoNavegador(pedidoDeLogin.url);
  // Confere o state e troca o code pelos tokens com o code_verifier (PKCE)
  const resposta = await oidcClient.processSigninResponse(await urlDeRetorno);
  return tokensDaResposta(resposta, agora);
}

/**
 * Logout sem navegador: o endpoint de logout do Keycloak aceita o refresh token por POST de client público, encerra a
 * sessão e invalida os tokens dela (validado na Fase 1 da BREW-23).
 */
function criarEncerramentoNoDesktop(oidcClient: OidcClient) {
  return async ({ refreshToken }: TokensDaSessao): Promise<void> => {
    const endpointDeLogout = await oidcClient.metadataService.getEndSessionEndpoint();
    if (endpointDeLogout === undefined) {
      throw new Error('O provedor não publica o end_session_endpoint');
    }
    const resposta = await fetch(endpointDeLogout, {
      method: 'POST',
      body: new URLSearchParams({ client_id: oidcClient.settings.client_id, refresh_token: refreshToken }),
    });
    if (!resposta.ok) {
      throw new Error(`Logout recusado pelo provedor: HTTP ${String(resposta.status)}`);
    }
  };
}

/** Página que o navegador mostra ao fim do login, no idioma do app; sem script, pela CSP do loopback */
function paginaDeRetorno(): string {
  const titulo = escaparHtml(i18next.t('app.sessao.retorno.titulo'));
  const mensagem = escaparHtml(i18next.t('app.sessao.retorno.mensagem'));
  return (
    `<!doctype html><html lang="${escaparHtml(i18next.language)}"><head><meta charset="utf-8"><title>${titulo}</title></head>` +
    `<body style="font-family:system-ui,sans-serif;text-align:center;padding:3rem">` +
    `<h1>${titulo}</h1><p>${mensagem}</p></body></html>`
  );
}

function escaparHtml(texto: string): string {
  return texto.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
}
