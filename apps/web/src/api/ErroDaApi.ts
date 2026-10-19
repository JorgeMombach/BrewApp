import { CabecalhosBrewApp, ehProblemDetails, type ErroDeValidacao, type ProblemDetails } from '@brewapp/shared';

/**
 * Códigos de falha que acontecem antes de existir um Problem Details: o backend nunca os devolve. A interface os
 * traduz como os códigos da API (design da API, seção 1.6).
 */
export const CodigoDeErroDoCliente = {
  /** A requisição não chegou ao servidor: sem rede, DNS, CORS ou servidor fora do ar */
  SEM_CONEXAO: 'SEM_CONEXAO',
  /** Resposta de erro fora do formato Problem Details, como a página de erro de um proxy */
  RESPOSTA_INESPERADA: 'RESPOSTA_INESPERADA',
} as const;

interface DadosDoErro {
  code: string;
  status: number | null;
  params: ProblemDetails['params'];
  errors: ErroDeValidacao[];
  correlationId: string | null;
  mensagemTecnica: string;
}

/**
 * Toda falha de chamada à API chega às telas como ErroDaApi, com ou sem resposta do servidor. A tela decide pelo code
 * e exibe a sua tradução; a message é só para log, como o title do Problem Details.
 */
export class ErroDaApi extends Error {
  override readonly name = 'ErroDaApi';
  /** Código do Problem Details ou um dos CodigoDeErroDoCliente */
  readonly code: string;
  /** Status HTTP da resposta; null quando a requisição não teve resposta */
  readonly status: number | null;
  readonly params: ProblemDetails['params'];
  /** Campos com problema nas falhas de validação; vazio nos demais erros */
  readonly errors: ErroDeValidacao[];
  /** Identificador para o usuário informar ao relatar o problema; null quando a requisição não teve resposta */
  readonly correlationId: string | null;

  private constructor(dadosDoErro: DadosDoErro, opcoesDoErro?: ErrorOptions) {
    super(dadosDoErro.mensagemTecnica, opcoesDoErro);
    this.code = dadosDoErro.code;
    this.status = dadosDoErro.status;
    this.params = dadosDoErro.params;
    this.errors = dadosDoErro.errors;
    this.correlationId = dadosDoErro.correlationId;
  }

  /** Lê o corpo de uma resposta de erro, que só é tratado como Problem Details se seguir o formato. */
  static async daResposta(resposta: Response): Promise<ErroDaApi> {
    const corpo = await lerJson(resposta);
    if (ehProblemDetails(corpo)) {
      return new ErroDaApi({
        code: corpo.code,
        status: resposta.status,
        params: corpo.params,
        errors: corpo.errors,
        correlationId: corpo.correlationId,
        mensagemTecnica: `${String(resposta.status)} ${corpo.code}: ${corpo.title}`,
      });
    }
    return new ErroDaApi({
      code: CodigoDeErroDoCliente.RESPOSTA_INESPERADA,
      status: resposta.status,
      params: {},
      errors: [],
      correlationId: resposta.headers.get(CabecalhosBrewApp.X_CORRELATION_ID),
      mensagemTecnica: `${String(resposta.status)} sem Problem Details no corpo`,
    });
  }

  static semConexao(causa: unknown): ErroDaApi {
    return new ErroDaApi(
      {
        code: CodigoDeErroDoCliente.SEM_CONEXAO,
        status: null,
        params: {},
        errors: [],
        correlationId: null,
        mensagemTecnica: 'Requisição sem resposta do servidor',
      },
      { cause: causa },
    );
  }
}

async function lerJson(resposta: Response): Promise<unknown> {
  try {
    const corpo: unknown = await resposta.json();
    return corpo;
  } catch {
    // Corpo vazio, HTML ou texto: quem chama trata como resposta fora do formato
    return undefined;
  }
}
