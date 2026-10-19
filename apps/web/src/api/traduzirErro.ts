import { chaveDaValidacao, chaveDoErro } from '@brewapp/shared';
import type { TFunction } from 'i18next';
import type { ErroDaApi } from './ErroDaApi';

export interface CampoComErro {
  /** Caminho do campo no corpo enviado (ex.: capacidade.valor), para a tela marcar o campo certo */
  campo: string;
  mensagem: string;
}

/** O que a tela exibe de um ErroDaApi, já no idioma em uso. */
export interface ErroTraduzido {
  mensagem: string;
  /** Texto com o correlationId para o usuário informar ao suporte; null quando a requisição não teve resposta */
  referencia: string | null;
  /** Campos com problema nas falhas de validação; vazio nos demais erros */
  campos: CampoComErro[];
}

/**
 * Traduz um ErroDaApi pelo code e pelos params (spec, seção 8): o backend não traduz nada, e o title nunca chega à
 * tela. Recebe o t do useTranslation, para a tela trocar de idioma junto com o resto da interface.
 */
export function traduzirErro(erroDaApi: ErroDaApi, t: TFunction): ErroTraduzido {
  return {
    mensagem: t(chaveDoErro(erroDaApi.code), erroDaApi.params),
    referencia:
      erroDaApi.correlationId === null ? null : t('erro.referencia', { correlationId: erroDaApi.correlationId }),
    campos: erroDaApi.errors.map((erroDeValidacao) => ({
      campo: erroDeValidacao.campo,
      mensagem: t(chaveDaValidacao(erroDeValidacao.code), erroDeValidacao.params),
    })),
  };
}
