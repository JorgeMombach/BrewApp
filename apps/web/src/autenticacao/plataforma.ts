import { isTauri } from '@tauri-apps/api/core';
import type { ConfiguracaoOidc } from './configuracaoOidc';
import { criarAutenticacaoNoDesktop } from './desktop/autenticacaoNoDesktop';
import type { GerenciadorDeSessao } from './GerenciadorDeSessao';
import { criarAutenticacaoNaWeb } from './web/autenticacaoNaWeb';

/** Sessão e login da plataforma em que o app está rodando: o mesmo app web serve a web e o desktop (Tauri). */
export interface AutenticacaoDaPlataforma {
  gerenciadorDeSessao: GerenciadorDeSessao;
  /** Leva o usuário ao login do Keycloak; no desktop, termina com a sessão registrada */
  entrar: () => Promise<void>;
  /** Desktop: o login segue no navegador do sistema, e o app fica esperando. Web: a própria página vai ao Keycloak */
  abreNavegadorExterno: boolean;
  /**
   * Só na web: processa a volta do Keycloak (rota /auth/callback) e devolve o caminho onde o usuário estava.
   * Chamadas com a mesma URL devolvem o mesmo resultado
   */
  concluirEntradaPorRedirect?: (urlDeRetorno: string) => Promise<string>;
}

export function criarAutenticacaoDaPlataforma(configuracao: ConfiguracaoOidc): AutenticacaoDaPlataforma {
  return isTauri() ? criarAutenticacaoNoDesktop(configuracao) : criarAutenticacaoNaWeb(configuracao);
}
