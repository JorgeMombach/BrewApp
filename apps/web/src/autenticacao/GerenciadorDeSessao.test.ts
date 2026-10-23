// @vitest-environment node
// Sem DOM: o gerenciador só lida com promessas, o relógio injetado e os fakes
import { describe, expect, it } from 'vitest';
import { GerenciadorDeSessao, MARGEM_DE_RENOVACAO_MS } from './GerenciadorDeSessao';
import {
  type CofreDaSessao,
  type CredenciaisDeRenovacao,
  SessaoEncerradaNoProvedor,
  type TokensDaSessao,
} from './sessao';

const INSTANTE_INICIAL = Date.UTC(2026, 9, 22, 12, 0, 0);
const CINCO_MINUTOS_MS = 5 * 60 * 1000;

class CofreEmMemoria implements CofreDaSessao {
  credenciais: CredenciaisDeRenovacao | null;

  constructor(credenciais: CredenciaisDeRenovacao | null = null) {
    this.credenciais = credenciais;
  }

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

class RelogioDeTeste {
  instante = INSTANTE_INICIAL;
  readonly agora = () => this.instante;

  avancar(milissegundos: number) {
    this.instante += milissegundos;
  }
}

/** Provedor falso: cada renovação devolve tokens novos (rotação), até ser programado para falhar. */
class ProvedorFalso {
  renovacoesFeitas: CredenciaisDeRenovacao[] = [];
  sessoesEncerradas: TokensDaSessao[] = [];
  falhaDaRenovacao: Error | null = null;
  falhaDoEncerramento: Error | null = null;
  /** Quando definido, a renovação só termina quando o teste chamar liberarRenovacao */
  segurarRenovacao = false;
  private liberacoesPendentes: (() => void)[] = [];

  constructor(private readonly relogio: RelogioDeTeste) {}

  readonly renovar = async (credenciais: CredenciaisDeRenovacao): Promise<TokensDaSessao> => {
    this.renovacoesFeitas.push(credenciais);
    if (this.segurarRenovacao) {
      await new Promise<void>((liberar) => this.liberacoesPendentes.push(liberar));
    }
    if (this.falhaDaRenovacao !== null) {
      throw this.falhaDaRenovacao;
    }
    return tokensValidos(this.relogio, `renovado-${String(this.renovacoesFeitas.length)}`);
  };

  readonly encerrarNoProvedor = (tokens: TokensDaSessao): Promise<void> => {
    this.sessoesEncerradas.push(tokens);
    return this.falhaDoEncerramento === null ? Promise.resolve() : Promise.reject(this.falhaDoEncerramento);
  };

  liberarRenovacao() {
    this.liberacoesPendentes.forEach((liberar) => {
      liberar();
    });
    this.liberacoesPendentes = [];
  }
}

function tokensValidos(relogio: RelogioDeTeste, sufixo: string): TokensDaSessao {
  return {
    tokenDeAcesso: `acesso-${sufixo}`,
    expiraEm: relogio.agora() + CINCO_MINUTOS_MS,
    refreshToken: `refresh-${sufixo}`,
    idToken: `id-${sufixo}`,
  };
}

function montarCenario(credenciaisGuardadas: CredenciaisDeRenovacao | null = null) {
  const relogio = new RelogioDeTeste();
  const cofre = new CofreEmMemoria(credenciaisGuardadas);
  const provedor = new ProvedorFalso(relogio);
  const gerenciadorDeSessao = new GerenciadorDeSessao({
    renovar: provedor.renovar,
    encerrarNoProvedor: provedor.encerrarNoProvedor,
    cofre,
    agora: relogio.agora,
  });
  return { relogio, cofre, provedor, gerenciadorDeSessao };
}

async function montarCenarioAutenticado() {
  const cenario = montarCenario();
  await cenario.gerenciadorDeSessao.iniciar();
  await cenario.gerenciadorDeSessao.registrarEntrada(tokensValidos(cenario.relogio, 'login'));
  return cenario;
}

const CREDENCIAIS_GUARDADAS: CredenciaisDeRenovacao = { refreshToken: 'refresh-guardado', idToken: 'id-guardado' };

describe('GerenciadorDeSessao', () => {
  describe('ao iniciar', () => {
    it('fica anônima quando o cofre está vazio', async () => {
      const { gerenciadorDeSessao, provedor } = montarCenario();

      expect(gerenciadorDeSessao.situacaoAtual()).toBe('iniciando');
      await gerenciadorDeSessao.iniciar();

      expect(gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBeNull();
      expect(provedor.renovacoesFeitas).toEqual([]);
    });

    it('restaura a sessão a partir do cofre e guarda o refresh token rotacionado', async () => {
      const { gerenciadorDeSessao, provedor, cofre } = montarCenario(CREDENCIAIS_GUARDADAS);

      await gerenciadorDeSessao.iniciar();

      expect(provedor.renovacoesFeitas).toEqual([CREDENCIAIS_GUARDADAS]);
      expect(gerenciadorDeSessao.situacaoAtual()).toBe('autenticada');
      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBe('acesso-renovado-1');
      expect(cofre.credenciais).toEqual({ refreshToken: 'refresh-renovado-1', idToken: 'id-renovado-1' });
    });

    it('apaga o cofre quando o provedor já encerrou a sessão', async () => {
      const { gerenciadorDeSessao, provedor, cofre } = montarCenario(CREDENCIAIS_GUARDADAS);
      provedor.falhaDaRenovacao = new SessaoEncerradaNoProvedor('invalid_grant');

      await gerenciadorDeSessao.iniciar();

      expect(gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
      expect(cofre.credenciais).toBeNull();
    });

    it('fica anônima sem apagar o cofre quando o provedor não responde, e repassa a falha', async () => {
      const { gerenciadorDeSessao, provedor, cofre } = montarCenario(CREDENCIAIS_GUARDADAS);
      const falhaDeRede = new TypeError('Failed to fetch');
      provedor.falhaDaRenovacao = falhaDeRede;

      await expect(gerenciadorDeSessao.iniciar()).rejects.toBe(falhaDeRede);

      expect(gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
      expect(cofre.credenciais).toEqual(CREDENCIAIS_GUARDADAS);
    });
  });

  describe('ao registrar a entrada', () => {
    it('fica autenticada, guarda as credenciais e avisa quem assina', async () => {
      const { gerenciadorDeSessao, relogio, cofre } = montarCenario();
      await gerenciadorDeSessao.iniciar();
      const situacoesAvisadas: string[] = [];
      gerenciadorDeSessao.assinar(() => situacoesAvisadas.push(gerenciadorDeSessao.situacaoAtual()));

      await gerenciadorDeSessao.registrarEntrada(tokensValidos(relogio, 'login'));

      expect(situacoesAvisadas).toEqual(['autenticada']);
      expect(cofre.credenciais).toEqual({ refreshToken: 'refresh-login', idToken: 'id-login' });
      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBe('acesso-login');
    });

    it('para de avisar quem cancelou a assinatura', async () => {
      const { gerenciadorDeSessao, relogio } = montarCenario();
      let avisos = 0;
      const cancelarAssinatura = gerenciadorDeSessao.assinar(() => (avisos += 1));
      await gerenciadorDeSessao.iniciar();

      cancelarAssinatura();
      await gerenciadorDeSessao.registrarEntrada(tokensValidos(relogio, 'login'));

      expect(avisos).toBe(1);
    });
  });

  describe('ao obter o token de acesso', () => {
    it('devolve o token em uso sem renovar enquanto ele está longe de expirar', async () => {
      const { gerenciadorDeSessao, provedor, relogio } = await montarCenarioAutenticado();
      relogio.avancar(CINCO_MINUTOS_MS - MARGEM_DE_RENOVACAO_MS - 1);

      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBe('acesso-login');
      expect(provedor.renovacoesFeitas).toEqual([]);
    });

    it('renova perto da expiração e guarda o refresh token rotacionado', async () => {
      const { gerenciadorDeSessao, provedor, relogio, cofre } = await montarCenarioAutenticado();
      relogio.avancar(CINCO_MINUTOS_MS - MARGEM_DE_RENOVACAO_MS);

      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBe('acesso-renovado-1');
      expect(provedor.renovacoesFeitas).toEqual([{ refreshToken: 'refresh-login', idToken: 'id-login' }]);
      expect(cofre.credenciais).toEqual({ refreshToken: 'refresh-renovado-1', idToken: 'id-renovado-1' });
    });

    it('faz uma renovação só para chamadas simultâneas, porque o refresh token vale uma vez', async () => {
      const { gerenciadorDeSessao, provedor, relogio } = await montarCenarioAutenticado();
      relogio.avancar(CINCO_MINUTOS_MS);

      const tokens = await Promise.all([
        gerenciadorDeSessao.obterTokenDeAcesso(),
        gerenciadorDeSessao.obterTokenDeAcesso(),
        gerenciadorDeSessao.obterTokenDeAcesso(),
      ]);

      expect(tokens).toEqual(['acesso-renovado-1', 'acesso-renovado-1', 'acesso-renovado-1']);
      expect(provedor.renovacoesFeitas).toHaveLength(1);
    });

    it('encerra a sessão local quando o provedor recusa a renovação', async () => {
      const { gerenciadorDeSessao, provedor, relogio, cofre } = await montarCenarioAutenticado();
      provedor.falhaDaRenovacao = new SessaoEncerradaNoProvedor('invalid_grant');
      relogio.avancar(CINCO_MINUTOS_MS);

      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBeNull();
      expect(gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
      expect(cofre.credenciais).toBeNull();
    });

    it('mantém a sessão e repassa a falha quando o provedor não responde', async () => {
      const { gerenciadorDeSessao, provedor, relogio } = await montarCenarioAutenticado();
      const falhaDeRede = new TypeError('Failed to fetch');
      provedor.falhaDaRenovacao = falhaDeRede;
      relogio.avancar(CINCO_MINUTOS_MS);

      await expect(gerenciadorDeSessao.obterTokenDeAcesso()).rejects.toBe(falhaDeRede);
      expect(gerenciadorDeSessao.situacaoAtual()).toBe('autenticada');

      provedor.falhaDaRenovacao = null;
      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBe('acesso-renovado-2');
    });
  });

  describe('ao sair', () => {
    it('limpa a memória e o cofre e encerra a sessão no provedor', async () => {
      const { gerenciadorDeSessao, provedor, cofre } = await montarCenarioAutenticado();

      await gerenciadorDeSessao.sair();

      expect(gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
      expect(await gerenciadorDeSessao.obterTokenDeAcesso()).toBeNull();
      expect(cofre.credenciais).toBeNull();
      expect(provedor.sessoesEncerradas.map((tokens) => tokens.refreshToken)).toEqual(['refresh-login']);
    });

    it('limpa a sessão local mesmo quando o provedor não responde, e repassa a falha', async () => {
      const { gerenciadorDeSessao, provedor, cofre } = await montarCenarioAutenticado();
      const falhaDeRede = new TypeError('Failed to fetch');
      provedor.falhaDoEncerramento = falhaDeRede;

      await expect(gerenciadorDeSessao.sair()).rejects.toBe(falhaDeRede);

      expect(gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
      expect(cofre.credenciais).toBeNull();
    });

    it('não chama o provedor quando não há sessão', async () => {
      const { gerenciadorDeSessao, provedor } = montarCenario();
      await gerenciadorDeSessao.iniciar();

      await gerenciadorDeSessao.sair();

      expect(provedor.sessoesEncerradas).toEqual([]);
    });

    it('não deixa uma renovação em andamento restaurar a sessão encerrada', async () => {
      const { gerenciadorDeSessao, provedor, relogio, cofre } = await montarCenarioAutenticado();
      provedor.segurarRenovacao = true;
      relogio.avancar(CINCO_MINUTOS_MS);
      const tokenPedidoAntesDeSair = gerenciadorDeSessao.obterTokenDeAcesso();

      await gerenciadorDeSessao.sair();
      provedor.liberarRenovacao();

      expect(await tokenPedidoAntesDeSair).toBeNull();
      expect(gerenciadorDeSessao.situacaoAtual()).toBe('anonima');
      expect(cofre.credenciais).toBeNull();
    });
  });
});
