import { Channel, invoke } from '@tauri-apps/api/core';
import type { CofreDaSessao, CredenciaisDeRenovacao } from '../sessao';

/** Comandos do src-tauri/src/cofre.rs: credenciais no cofre do sistema operacional (spec 9.5). */
export const cofreDoDesktop: CofreDaSessao = {
  ler: () => invoke<CredenciaisDeRenovacao | null>('ler_credenciais'),
  guardar: (credenciais) => invoke('guardar_credenciais', { credenciais }),
  apagar: () => invoke('apagar_credenciais'),
};

export interface RetornoDoLogin {
  /** Porta em que o servidor de loopback escuta, para montar o redirect_uri */
  porta: number;
  /** URL que o Keycloak chamou ao fim do login, com code e state */
  urlDeRetorno: Promise<string>;
}

/**
 * Sobe o servidor de loopback do src-tauri/src/retorno_do_login.rs e termina assim que ele avisa a porta; a URL do
 * retorno chega depois, quando o usuário concluir o login no navegador. O navegador recebe a página informada.
 */
export function escutarRetornoDoLogin(paginaDeRetorno: string): Promise<RetornoDoLogin> {
  return new Promise((resolver, rejeitar) => {
    const aoEscutar = new Channel<number>();
    const urlDeRetorno = invoke<string>('receber_retorno_do_login', { paginaDeRetorno, aoEscutar });
    aoEscutar.onmessage = (porta) => {
      resolver({ porta, urlDeRetorno });
    };
    // Falha antes de avisar a porta rejeita aqui; depois disso, quem rejeita é a urlDeRetorno, e esta linha não muda nada
    urlDeRetorno.catch(rejeitar);
  });
}
