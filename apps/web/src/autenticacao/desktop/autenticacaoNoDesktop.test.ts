// @vitest-environment node
// O Keycloak é um fetch falso; o cofre, o loopback e o navegador são fakes dos recursos nativos
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { CofreDaSessao, CredenciaisDeRenovacao } from '../sessao';
import { criarAutenticacaoNoDesktop, type RecursosNativos } from './autenticacaoNoDesktop';

const EMISSOR = 'http://localhost:8180/realms/brewapp';
const ENDPOINT_DE_TOKEN = `${EMISSOR}/protocol/openid-connect/token`;
const ENDPOINT_DE_LOGOUT = `${EMISSOR}/protocol/openid-connect/logout`;
const PORTA_DO_LOOPBACK = 50123;
const INSTANTE = Date.UTC(2026, 9, 22, 12, 0, 0);
const CONFIGURACAO = { emissor: EMISSOR, clientIdWeb: 'brewapp-web', clientIdDesktop: 'brewapp-desktop' };

function jwtDeTeste(sub: string): string {
  const parte = (conteudo: object) =>
    btoa(JSON.stringify(conteudo)).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
  const segundos = Math.floor(INSTANTE / 1000);
  return `${parte({ alg: 'none' })}.${parte({ iss: EMISSOR, sub, aud: 'brewapp-desktop', iat: segundos, exp: segundos + 300 })}.`;
}

class CofreEmMemoria implements CofreDaSessao {
  constructor(public credenciais: CredenciaisDeRenovacao | null = null) {}

  ler() {
    return Promise.resolve(this.credenciais);
  }

  guardar(credenciais: CredenciaisDeRenovacao) {
    this.credenciais = credenciais;
    return Promise.resolve();
  }

  apagar() {
    this.credenciais = null;
    return Promise.resolve();
  }
}

/** Keycloak falso: descoberta, endpoint de token e endpoint de logout, guardando os formulários recebidos. */
function simularKeycloak({ statusDoLogout = 204 } = {}) {
  const formularios: Record<'token' | 'logout', URLSearchParams[]> = { token: [], logout: [] };
  let renovacoes = 0;
  vi.stubGlobal('fetch', (url: string, opcoes?: RequestInit) => {
    const formulario = opcoes?.body instanceof URLSearchParams ? opcoes.body : new URLSearchParams();
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
    if (url === ENDPOINT_DE_TOKEN) {
      formularios.token.push(formulario);
      renovacoes += 1;
      return Promise.resolve(
        Response.json({
          access_token: `acesso-${String(renovacoes)}`,
          token_type: 'Bearer',
          expires_in: 300,
          refresh_token: `refresh-${String(renovacoes)}`,
          id_token: jwtDeTeste('usuario-1'),
        }),
      );
    }
    if (url === ENDPOINT_DE_LOGOUT) {
      formularios.logout.push(formulario);
      return Promise.resolve(new Response(null, { status: statusDoLogout }));
    }
    return Promise.reject(new Error(`URL inesperada no teste: ${url}`));
  });
  return formularios;
}

/** Recursos nativos falsos: o navegador "conclui o login" devolvendo o code no loopback com o state recebido. */
function criarRecursosNativos(cofre = new CofreEmMemoria()) {
  const registro: { paginaDeRetorno: string; urlAberta: URL | null } = { paginaDeRetorno: '', urlAberta: null };
  let entregarRetorno: (url: string) => void = () => undefined;
  const recursosNativos: RecursosNativos = {
    cofre,
    escutarRetornoDoLogin: (paginaDeRetorno) => {
      registro.paginaDeRetorno = paginaDeRetorno;
      const urlDeRetorno = new Promise<string>((resolver) => (entregarRetorno = resolver));
      return Promise.resolve({ porta: PORTA_DO_LOOPBACK, urlDeRetorno });
    },
    abrirNoNavegador: (url) => {
      const urlAberta = new URL(url);
      registro.urlAberta = urlAberta;
      const redirectUri = urlAberta.searchParams.get('redirect_uri') ?? '';
      const state = urlAberta.searchParams.get('state') ?? '';
      entregarRetorno(`${redirectUri}?code=code-de-teste&state=${state}`);
      return Promise.resolve();
    },
  };
  return { recursosNativos, registro, cofre };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('autenticação no desktop', () => {
  it('entra pelo navegador do sistema com PKCE e retorno no loopback, e guarda as credenciais no cofre', async () => {
    const formularios = simularKeycloak();
    const { recursosNativos, registro, cofre } = criarRecursosNativos();
    const autenticacao = criarAutenticacaoNoDesktop(CONFIGURACAO, recursosNativos, () => INSTANTE);
    await autenticacao.gerenciadorDeSessao.iniciar();

    await autenticacao.entrar();

    const parametrosDoLogin = registro.urlAberta?.searchParams;
    expect(parametrosDoLogin?.get('client_id')).toBe('brewapp-desktop');
    expect(parametrosDoLogin?.get('redirect_uri')).toBe(`http://127.0.0.1:${String(PORTA_DO_LOOPBACK)}/auth/callback`);
    expect(parametrosDoLogin?.get('code_challenge_method')).toBe('S256');
    expect(parametrosDoLogin?.get('ui_locales')).toBe('pt-BR');

    const troca = formularios.token[0];
    expect(troca?.get('grant_type')).toBe('authorization_code');
    expect(troca?.get('code')).toBe('code-de-teste');
    expect(troca?.get('code_verifier')).toMatch(/^[\w-]{43,}$/);
    expect(troca?.get('redirect_uri')).toBe(parametrosDoLogin?.get('redirect_uri'));

    expect(autenticacao.gerenciadorDeSessao.situacaoAtual()).toBe('autenticada');
    expect(await autenticacao.gerenciadorDeSessao.obterTokenDeAcesso()).toBe('acesso-1');
    expect(cofre.credenciais?.refreshToken).toBe('refresh-1');
    expect(registro.paginaDeRetorno).toContain('<h1>Login concluído</h1>');
  });

  it('restaura a sessão a partir do cofre ao abrir o app', async () => {
    const formularios = simularKeycloak();
    const cofre = new CofreEmMemoria({ refreshToken: 'refresh-guardado', idToken: jwtDeTeste('usuario-1') });
    const { recursosNativos } = criarRecursosNativos(cofre);
    const autenticacao = criarAutenticacaoNoDesktop(CONFIGURACAO, recursosNativos, () => INSTANTE);

    await autenticacao.gerenciadorDeSessao.iniciar();

    expect(formularios.token[0]?.get('refresh_token')).toBe('refresh-guardado');
    expect(autenticacao.gerenciadorDeSessao.situacaoAtual()).toBe('autenticada');
    expect(cofre.credenciais?.refreshToken).toBe('refresh-1');
  });

  it('sai encerrando a sessão no Keycloak por POST com o refresh token, sem abrir o navegador', async () => {
    const formularios = simularKeycloak();
    const { recursosNativos, cofre } = criarRecursosNativos();
    const autenticacao = criarAutenticacaoNoDesktop(CONFIGURACAO, recursosNativos, () => INSTANTE);
    await autenticacao.gerenciadorDeSessao.iniciar();
    await autenticacao.entrar();

    await autenticacao.gerenciadorDeSessao.sair();

    expect(formularios.logout[0]?.get('client_id')).toBe('brewapp-desktop');
    expect(formularios.logout[0]?.get('refresh_token')).toBe('refresh-1');
    expect(autenticacao.gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
    expect(cofre.credenciais).toBeNull();
  });

  it('repassa a recusa do logout pelo Keycloak, com a sessão local já encerrada', async () => {
    simularKeycloak({ statusDoLogout: 400 });
    const { recursosNativos, cofre } = criarRecursosNativos();
    const autenticacao = criarAutenticacaoNoDesktop(CONFIGURACAO, recursosNativos, () => INSTANTE);
    await autenticacao.gerenciadorDeSessao.iniciar();
    await autenticacao.entrar();

    await expect(autenticacao.gerenciadorDeSessao.sair()).rejects.toThrow('HTTP 400');

    expect(autenticacao.gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
    expect(cofre.credenciais).toBeNull();
  });
});
