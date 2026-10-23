import { jwtDecode } from 'jwt-decode';
import {
  ErrorResponse,
  type IdTokenClaims,
  type OidcClient,
  type RefreshState,
  type SigninResponse,
} from 'oidc-client-ts';
import { type CredenciaisDeRenovacao, SessaoEncerradaNoProvedor, type TokensDaSessao } from './sessao';

/**
 * Converte a resposta do provedor (login ou renovação) nos tokens da sessão. A expiração é calculada pelo relógio
 * injetado a partir do expires_in, e não pelo expires_at, que o oidc-client-ts calcula com o relógio dele.
 */
export function tokensDaResposta(resposta: SigninResponse, agora: () => number): TokensDaSessao {
  const { access_token: tokenDeAcesso, expires_in: expiraEmSegundos, refresh_token, id_token } = resposta;
  if (expiraEmSegundos === undefined || refresh_token === undefined || id_token === undefined) {
    // O realm sempre devolve os três com o escopo openid: faltar algum é configuração errada do client
    throw new Error('Resposta do provedor sem expires_in, refresh_token ou id_token');
  }
  return { tokenDeAcesso, expiraEm: agora() + expiraEmSegundos * 1000, refreshToken: refresh_token, idToken: id_token };
}

/**
 * Renovação pelo refresh token, igual na web e no desktop. O id token anterior vai junto para o oidc-client-ts conferir
 * que o novo é do mesmo usuário. A recusa do provedor (invalid_grant) vira SessaoEncerradaNoProvedor; falha de rede
 * segue como veio.
 */
export function criarRenovacao(oidcClient: OidcClient, agora: () => number) {
  return async ({ refreshToken, idToken }: CredenciaisDeRenovacao): Promise<TokensDaSessao> => {
    // O RefreshState está nos tipos do oidc-client-ts 3.5, mas não é exportado em tempo de execução. Como a classe só
    // tem campos públicos, um objeto com o mesmo formato serve, e o compilador confere o formato
    const estadoDaRenovacao: RefreshState = {
      refresh_token: refreshToken,
      id_token: idToken,
      session_state: null,
      // Só decodifica, sem conferir assinatura: o id token veio do próprio provedor, e quem valida token é o backend
      profile: jwtDecode<IdTokenClaims>(idToken),
    };
    try {
      return tokensDaResposta(await oidcClient.useRefreshToken({ state: estadoDaRenovacao }), agora);
    } catch (erro) {
      if (erro instanceof ErrorResponse && erro.error === 'invalid_grant') {
        throw new SessaoEncerradaNoProvedor(erro.error_description ?? erro.error, { cause: erro });
      }
      throw erro;
    }
  };
}
