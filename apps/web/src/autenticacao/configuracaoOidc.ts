export interface ConfiguracaoOidc {
  /** Emissor dos tokens (realm do Keycloak), o mesmo que o backend valida */
  emissor: string;
  clientIdWeb: string;
  clientIdDesktop: string;
}

type VariaveisOidc = Pick<
  ImportMetaEnv,
  'VITE_OAUTH_ISSUER_URI' | 'VITE_OAUTH_CLIENT_ID_WEB' | 'VITE_OAUTH_CLIENT_ID_DESKTOP'
>;

/** Lê a configuração do .env; se faltar alguma variável, o app não sobe, como o backend (application.yml). */
export function lerConfiguracaoOidc(variaveis: VariaveisOidc = import.meta.env): ConfiguracaoOidc {
  return {
    emissor: exigir(variaveis.VITE_OAUTH_ISSUER_URI, 'VITE_OAUTH_ISSUER_URI'),
    clientIdWeb: exigir(variaveis.VITE_OAUTH_CLIENT_ID_WEB, 'VITE_OAUTH_CLIENT_ID_WEB'),
    clientIdDesktop: exigir(variaveis.VITE_OAUTH_CLIENT_ID_DESKTOP, 'VITE_OAUTH_CLIENT_ID_DESKTOP'),
  };
}

function exigir(valor: string | undefined, nomeDaVariavel: string): string {
  if (valor === undefined || valor.trim() === '') {
    throw new Error(`Variável ${nomeDaVariavel} ausente no .env da raiz (ver .env.example)`);
  }
  return valor;
}
