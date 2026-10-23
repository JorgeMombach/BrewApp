// O Keycloak é um fetch falso; o navegador entra pelos recursos injetados, com um Storage em memória do oidc-client-ts
import { InMemoryWebStorage } from 'oidc-client-ts';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { criarAutenticacaoNaWeb, type RecursosDoNavegador } from './autenticacaoNaWeb';

const EMISSOR = 'http://localhost:8180/realms/brewapp';
const ENDPOINT_DE_TOKEN = `${EMISSOR}/protocol/openid-connect/token`;
const ENDPOINT_DE_LOGOUT = `${EMISSOR}/protocol/openid-connect/logout`;
const ORIGEM = 'http://localhost:5173';
const INSTANTE = Date.UTC(2026, 9, 22, 12, 0, 0);
const CONFIGURACAO = { emissor: EMISSOR, clientIdWeb: 'brewapp-web', clientIdDesktop: 'brewapp-desktop' };
const ID_TOKEN = jwtDeTeste('usuario-1');

function jwtDeTeste(sub: string): string {
  const parte = (conteudo: object) =>
    btoa(JSON.stringify(conteudo)).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
  const segundos = Math.floor(INSTANTE / 1000);
  return `${parte({ alg: 'none' })}.${parte({ iss: EMISSOR, sub, aud: 'brewapp-web', iat: segundos, exp: segundos + 300 })}.`;
}

function simularKeycloak() {
  const formulariosDeToken: URLSearchParams[] = [];
  vi.stubGlobal('fetch', (url: string, opcoes?: RequestInit) => {
    if (url === `${EMISSOR}/.well-known/openid-configuration`) {
      return Promise.resolve(
        Response.json({
          issuer: EMISSOR,
          authorization_endpoint: `${EMISSOR}/protocol/openid-connect/auth`,
          token_endpoint: ENDPOINT_DE_TOKEN,
          end_session_endpoint: ENDPOINT_DE_LOGOUT,
        }),
      );
    }
    if (url === ENDPOINT_DE_TOKEN && opcoes?.body instanceof URLSearchParams) {
      formulariosDeToken.push(opcoes.body);
      return Promise.resolve(
        Response.json({
          access_token: 'token-de-acesso-secreto',
          token_type: 'Bearer',
          expires_in: 300,
          refresh_token: 'refresh-token-secreto',
          id_token: ID_TOKEN,
        }),
      );
    }
    return Promise.reject(new Error(`URL inesperada no teste: ${url}`));
  });
  return formulariosDeToken;
}

function criarRecursos(caminhoAtual = '/') {
  const urlsNavegadas: URL[] = [];
  const armazenamentoDoLogin = new InMemoryWebStorage();
  const recursos: RecursosDoNavegador = {
    origem: ORIGEM,
    navegarPara: (url) => urlsNavegadas.push(new URL(url)),
    caminhoAtual: () => caminhoAtual,
    armazenamentoDoLogin,
  };
  return { recursos, urlsNavegadas, armazenamentoDoLogin };
}

/** Entra e volta do Keycloak com o code e o state que ele receberia */
async function entrarEVoltar(caminhoAtual = '/') {
  const formulariosDeToken = simularKeycloak();
  const { recursos, urlsNavegadas, armazenamentoDoLogin } = criarRecursos(caminhoAtual);
  const autenticacao = criarAutenticacaoNaWeb(CONFIGURACAO, recursos, () => INSTANTE);
  await autenticacao.gerenciadorDeSessao.iniciar();
  await autenticacao.entrar();
  const state = urlsNavegadas[0]?.searchParams.get('state') ?? '';
  const urlDeRetorno = `${ORIGEM}/auth/callback?code=code-de-teste&state=${state}`;
  return { autenticacao, urlsNavegadas, armazenamentoDoLogin, formulariosDeToken, urlDeRetorno };
}

function valoresGuardados(armazenamento: Storage): string {
  return Array.from({ length: armazenamento.length }, (_, indice) => {
    const chave = armazenamento.key(indice) ?? '';
    return `${chave}=${armazenamento.getItem(chave) ?? ''}`;
  }).join('\n');
}

afterEach(() => {
  vi.unstubAllGlobals();
  localStorage.clear();
  sessionStorage.clear();
});

describe('autenticação na web', () => {
  it('redireciona ao Keycloak com PKCE, o retorno cadastrado e o idioma do app', async () => {
    const { urlsNavegadas } = await entrarEVoltar();

    const parametrosDoLogin = urlsNavegadas[0]?.searchParams;
    expect(urlsNavegadas[0]?.origin).toBe('http://localhost:8180');
    expect(parametrosDoLogin?.get('client_id')).toBe('brewapp-web');
    expect(parametrosDoLogin?.get('redirect_uri')).toBe(`${ORIGEM}/auth/callback`);
    expect(parametrosDoLogin?.get('code_challenge_method')).toBe('S256');
    expect(parametrosDoLogin?.get('ui_locales')).toBe('pt-BR');
  });

  it('conclui o retorno, abre a sessão e devolve a página onde o usuário estava', async () => {
    const { autenticacao, formulariosDeToken, urlDeRetorno } = await entrarEVoltar('/lotes?situacao=ativo');

    const destino = await autenticacao.concluirEntradaPorRedirect?.(urlDeRetorno);

    expect(destino).toBe('/lotes?situacao=ativo');
    expect(formulariosDeToken[0]?.get('code_verifier')).toMatch(/^[\w-]{43,}$/);
    expect(autenticacao.gerenciadorDeSessao.situacaoAtual()).toBe('autenticada');
    expect(await autenticacao.gerenciadorDeSessao.obterTokenDeAcesso()).toBe('token-de-acesso-secreto');
  });

  it('guarda os tokens só em memória: nada no localStorage, no sessionStorage nem no armazenamento do login', async () => {
    const { autenticacao, armazenamentoDoLogin, urlDeRetorno } = await entrarEVoltar();

    await autenticacao.concluirEntradaPorRedirect?.(urlDeRetorno);

    for (const armazenamento of [armazenamentoDoLogin, localStorage, sessionStorage]) {
      expect(valoresGuardados(armazenamento)).not.toContain('secreto');
    }
  });

  it('processa o mesmo retorno uma vez só, mesmo chamado de novo (StrictMode)', async () => {
    const { autenticacao, formulariosDeToken, urlDeRetorno } = await entrarEVoltar();

    const destinos = await Promise.all([
      autenticacao.concluirEntradaPorRedirect?.(urlDeRetorno),
      autenticacao.concluirEntradaPorRedirect?.(urlDeRetorno),
    ]);

    expect(destinos).toEqual(['/', '/']);
    expect(formulariosDeToken).toHaveLength(1);
  });

  it('recusa um retorno com state que o app não gerou', async () => {
    const { autenticacao } = await entrarEVoltar();

    await expect(
      autenticacao.concluirEntradaPorRedirect?.(`${ORIGEM}/auth/callback?code=code-de-teste&state=forjado`),
    ).rejects.toThrow();
    expect(autenticacao.gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
  });

  it('sai pelo logout do Keycloak, com o id token e a volta para a página inicial', async () => {
    const { autenticacao, urlsNavegadas, urlDeRetorno } = await entrarEVoltar();
    await autenticacao.concluirEntradaPorRedirect?.(urlDeRetorno);

    await autenticacao.gerenciadorDeSessao.sair();

    const urlDeLogout = urlsNavegadas[1];
    expect(`${urlDeLogout?.origin ?? ''}${urlDeLogout?.pathname ?? ''}`).toBe(ENDPOINT_DE_LOGOUT);
    expect(urlDeLogout?.searchParams.get('client_id')).toBe('brewapp-web');
    expect(urlDeLogout?.searchParams.get('id_token_hint')).toBe(ID_TOKEN);
    expect(urlDeLogout?.searchParams.get('post_logout_redirect_uri')).toBe(`${ORIGEM}/`);
    expect(autenticacao.gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
  });
});
