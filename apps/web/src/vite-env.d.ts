// Variáveis do .env da raiz que o app lê (vite.config.ts, envDir). Opcionais: quem lê confere se vieram
interface ImportMetaEnv {
  readonly VITE_OAUTH_ISSUER_URI?: string;
  readonly VITE_OAUTH_CLIENT_ID_WEB?: string;
  readonly VITE_OAUTH_CLIENT_ID_DESKTOP?: string;
}
