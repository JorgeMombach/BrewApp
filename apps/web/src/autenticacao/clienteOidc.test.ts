// @vitest-environment node
// Só fetch e Response: o oidc-client-ts conversa com o endpoint de token por um fetch falso
import { InMemoryWebStorage, OidcClient, SigninResponse, WebStorageStateStore } from 'oidc-client-ts';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { criarRenovacao, tokensDaResposta } from './clienteOidc';
import { SessaoEncerradaNoProvedor } from './sessao';

const EMISSOR = 'http://localhost:8180/realms/brewapp';
const ENDPOINT_DE_TOKEN = `${EMISSOR}/protocol/openid-connect/token`;
const INSTANTE = Date.UTC(2026, 9, 22, 12, 0, 0);
const agora = () => INSTANTE;

/** JWT sem assinatura: o oidc-client-ts só decodifica o id token, quem confere assinatura é o backend. */
function jwtDeTeste(sub: string): string {
  const parte = (conteudo: object) =>
    btoa(JSON.stringify(conteudo)).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
  const segundos = Math.floor(INSTANTE / 1000);
  return `${parte({ alg: 'none' })}.${parte({ iss: EMISSOR, sub, aud: 'brewapp-desktop', iat: segundos, exp: segundos + 300 })}.`;
}

function criarOidcClient() {
  return new OidcClient({
    authority: EMISSOR,
    client_id: 'brewapp-desktop',
    redirect_uri: 'http://127.0.0.1/auth/callback',
    // Metadados fixos: sem buscar o .well-known no teste
    metadata: { issuer: EMISSOR, token_endpoint: ENDPOINT_DE_TOKEN, authorization_endpoint: `${EMISSOR}/auth` },
    stateStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  });
}

function responderNoEndpointDeToken(corpo: object, status: number) {
  const requisicoesFeitas: URLSearchParams[] = [];
  vi.stubGlobal('fetch', (url: string, opcoes: RequestInit) => {
    expect(url).toBe(ENDPOINT_DE_TOKEN);
    // O oidc-client-ts manda o formulário como URLSearchParams
    if (opcoes.body instanceof URLSearchParams) {
      requisicoesFeitas.push(opcoes.body);
    }
    return Promise.resolve(
      new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } }),
    );
  });
  return requisicoesFeitas;
}

const CREDENCIAIS = { refreshToken: 'refresh-antigo', idToken: jwtDeTeste('usuario-1') };

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('criarRenovacao', () => {
  it('troca o refresh token por tokens novos, com a expiração pelo relógio injetado', async () => {
    const idTokenNovo = jwtDeTeste('usuario-1');
    const requisicoesFeitas = responderNoEndpointDeToken(
      {
        access_token: 'acesso-novo',
        token_type: 'Bearer',
        expires_in: 300,
        refresh_token: 'refresh-novo',
        id_token: idTokenNovo,
      },
      200,
    );

    const tokens = await criarRenovacao(criarOidcClient(), agora)(CREDENCIAIS);

    expect(tokens).toEqual({
      tokenDeAcesso: 'acesso-novo',
      expiraEm: INSTANTE + 300_000,
      refreshToken: 'refresh-novo',
      idToken: idTokenNovo,
    });
    expect(requisicoesFeitas[0]?.get('grant_type')).toBe('refresh_token');
    expect(requisicoesFeitas[0]?.get('refresh_token')).toBe('refresh-antigo');
    expect(requisicoesFeitas[0]?.get('client_id')).toBe('brewapp-desktop');
  });

  it('recusa um id token novo de outro usuário', async () => {
    responderNoEndpointDeToken(
      {
        access_token: 'acesso-novo',
        token_type: 'Bearer',
        expires_in: 300,
        refresh_token: 'refresh-novo',
        id_token: jwtDeTeste('outro-usuario'),
      },
      200,
    );

    await expect(criarRenovacao(criarOidcClient(), agora)(CREDENCIAIS)).rejects.toThrow('sub');
  });

  it('traduz a recusa do provedor (invalid_grant) em SessaoEncerradaNoProvedor', async () => {
    responderNoEndpointDeToken({ error: 'invalid_grant', error_description: 'Token is not active' }, 400);

    await expect(criarRenovacao(criarOidcClient(), agora)(CREDENCIAIS)).rejects.toBeInstanceOf(
      SessaoEncerradaNoProvedor,
    );
  });

  it('repassa a falha de rede como veio, para a sessão não ser encerrada', async () => {
    const falhaDeRede = new TypeError('Failed to fetch');
    vi.stubGlobal('fetch', () => Promise.reject(falhaDeRede));

    const erro: unknown = await criarRenovacao(criarOidcClient(), agora)(CREDENCIAIS).catch((falha: unknown) => falha);

    expect(erro).not.toBeInstanceOf(SessaoEncerradaNoProvedor);
  });
});

describe('tokensDaResposta', () => {
  it('falha quando a resposta não traz o refresh token', () => {
    const resposta = new SigninResponse(new URLSearchParams());
    Object.assign(resposta, { access_token: 'acesso', expires_in: 300, id_token: jwtDeTeste('usuario-1') });

    expect(() => tokensDaResposta(resposta, agora)).toThrow('refresh_token');
  });
});
