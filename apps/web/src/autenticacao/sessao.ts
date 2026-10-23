/** Tokens da sessão em uso. Ficam só em memória (spec 9.5); no desktop, o cofre guarda o necessário para renovar. */
export interface TokensDaSessao {
  tokenDeAcesso: string;
  /** Instante em que o token de acesso expira, em milissegundos desde a época */
  expiraEm: number;
  refreshToken: string;
  /** Enviado ao renovar, para o provedor conferir que a sessão continua sendo do mesmo usuário */
  idToken: string;
}

/** O que basta para renovar a sessão sem um novo login: é o que o desktop guarda no cofre do sistema. */
export type CredenciaisDeRenovacao = Pick<TokensDaSessao, 'refreshToken' | 'idToken'>;

/**
 * Onde as credenciais de renovação sobrevivem ao fechamento do app: no desktop, o armazenamento seguro do sistema
 * operacional; na web, lugar nenhum (o token fica só em memória, spec 9.5).
 */
export interface CofreDaSessao {
  ler(): Promise<CredenciaisDeRenovacao | null>;
  guardar(credenciais: CredenciaisDeRenovacao): Promise<void>;
  apagar(): Promise<void>;
}

export type SituacaoDaSessao = 'iniciando' | 'anonima' | 'autenticada';

/**
 * O provedor recusou a renovação (invalid_grant): a sessão acabou por logout, por inatividade ou porque o refresh
 * token já tinha sido usado (rotação). Só um novo login resolve. Falha de rede chega como outro erro.
 */
export class SessaoEncerradaNoProvedor extends Error {
  override readonly name = 'SessaoEncerradaNoProvedor';
}
