import {
  type CofreDaSessao,
  type CredenciaisDeRenovacao,
  SessaoEncerradaNoProvedor,
  type SituacaoDaSessao,
  type TokensDaSessao,
} from './sessao';

/** Antecedência com que o token de acesso é renovado: evita mandar à API um token que expira no caminho. */
export const MARGEM_DE_RENOVACAO_MS = 30_000;

export interface DependenciasDoGerenciadorDeSessao {
  /** Troca o refresh token por tokens novos; rejeita com SessaoEncerradaNoProvedor se o provedor recusar */
  renovar: (credenciais: CredenciaisDeRenovacao) => Promise<TokensDaSessao>;
  /** Encerra a sessão no provedor: na web, redirecionando; no desktop, por chamada direta */
  encerrarNoProvedor: (tokens: TokensDaSessao) => Promise<void>;
  cofre: CofreDaSessao;
  /** Relógio injetado, em milissegundos desde a época (Date.now em produção) */
  agora: () => number;
}

/**
 * Mantém a sessão do usuário em memória, igual na web e no desktop; o que muda entre eles chega pelas dependências.
 * A renovação é feita sob demanda, quando alguém pede o token perto da expiração, e uma de cada vez: com a rotação
 * ligada no Keycloak, cada refresh token vale uma única vez, e duas renovações em paralelo derrubariam a sessão.
 * A situação segue o contrato do useSyncExternalStore (situacaoAtual e assinar).
 */
export class GerenciadorDeSessao {
  private tokens: TokensDaSessao | null = null;
  private situacao: SituacaoDaSessao = 'iniciando';
  private renovacaoEmAndamento: Promise<TokensDaSessao | null> | null = null;
  /** Muda a cada entrada e saída: uma renovação iniciada antes de um logout não restaura a sessão encerrada */
  private geracaoDaSessao = 0;
  private readonly ouvintes = new Set<() => void>();

  constructor(private readonly dependencias: DependenciasDoGerenciadorDeSessao) {}

  readonly situacaoAtual = (): SituacaoDaSessao => this.situacao;

  readonly assinar = (ouvinte: () => void): (() => void) => {
    this.ouvintes.add(ouvinte);
    return () => {
      this.ouvintes.delete(ouvinte);
    };
  };

  /**
   * Restaura a sessão a partir do cofre, quando ele guarda credenciais. Se o provedor não responder, a sessão fica
   * anônima sem apagar o cofre, para a próxima abertura tentar de novo, e a falha é repassada.
   */
  async iniciar(): Promise<void> {
    const credenciais = await this.dependencias.cofre.ler();
    if (credenciais === null) {
      this.mudarSituacao('anonima');
      return;
    }
    try {
      await this.renovarUmaVez(credenciais);
    } catch (erro) {
      this.mudarSituacao('anonima');
      throw erro;
    }
  }

  /** Recebe os tokens de um login concluído pelo fluxo da plataforma (redirect na web, navegador no desktop). */
  async registrarEntrada(tokens: TokensDaSessao): Promise<void> {
    this.geracaoDaSessao += 1;
    this.renovacaoEmAndamento = null;
    await this.adotar(tokens);
  }

  /**
   * Token de acesso válido para mandar à API, renovado antes quando está perto de expirar; null sem sessão ou quando o
   * provedor recusou a renovação. Falha de rede na renovação é repassada e mantém a sessão.
   */
  async obterTokenDeAcesso(): Promise<string | null> {
    if (this.tokens === null) {
      return null;
    }
    if (this.dependencias.agora() < this.tokens.expiraEm - MARGEM_DE_RENOVACAO_MS) {
      return this.tokens.tokenDeAcesso;
    }
    const { refreshToken, idToken } = this.tokens;
    const tokensRenovados = await this.renovarUmaVez({ refreshToken, idToken });
    return tokensRenovados?.tokenDeAcesso ?? null;
  }

  /**
   * Limpa a sessão local antes de avisar o provedor: sem rede, o usuário sai do mesmo jeito, e a falha do provedor é
   * repassada para quem chamou decidir o que mostrar.
   */
  async sair(): Promise<void> {
    const tokensDaSessaoEncerrada = this.tokens;
    await this.limparSessao();
    if (tokensDaSessaoEncerrada !== null) {
      await this.dependencias.encerrarNoProvedor(tokensDaSessaoEncerrada);
    }
  }

  private renovarUmaVez(credenciais: CredenciaisDeRenovacao): Promise<TokensDaSessao | null> {
    this.renovacaoEmAndamento ??= this.renovar(credenciais).finally(() => {
      this.renovacaoEmAndamento = null;
    });
    return this.renovacaoEmAndamento;
  }

  private async renovar(credenciais: CredenciaisDeRenovacao): Promise<TokensDaSessao | null> {
    const geracaoNoInicio = this.geracaoDaSessao;
    try {
      const tokensRenovados = await this.dependencias.renovar(credenciais);
      if (geracaoNoInicio !== this.geracaoDaSessao) {
        return null;
      }
      await this.adotar(tokensRenovados);
      return tokensRenovados;
    } catch (erro) {
      if (!(erro instanceof SessaoEncerradaNoProvedor)) {
        throw erro;
      }
      if (geracaoNoInicio === this.geracaoDaSessao) {
        await this.limparSessao();
      }
      return null;
    }
  }

  private async adotar(tokens: TokensDaSessao): Promise<void> {
    this.tokens = tokens;
    // Com a rotação, o refresh token anterior deixou de valer: o cofre precisa sempre do último
    await this.dependencias.cofre.guardar({ refreshToken: tokens.refreshToken, idToken: tokens.idToken });
    this.mudarSituacao('autenticada');
  }

  private async limparSessao(): Promise<void> {
    this.geracaoDaSessao += 1;
    this.tokens = null;
    this.renovacaoEmAndamento = null;
    await this.dependencias.cofre.apagar();
    this.mudarSituacao('anonima');
  }

  private mudarSituacao(situacao: SituacaoDaSessao) {
    if (this.situacao === situacao) {
      return;
    }
    this.situacao = situacao;
    this.ouvintes.forEach((ouvinte) => {
      ouvinte();
    });
  }
}
