import en from '../../i18n/en.json';
import es from '../../i18n/es.json';
import ptBR from '../../i18n/pt-BR.json';
import type { Grandeza, Unidade } from '../unidades/catalogo';
import type { Idioma } from './idiomas';

/**
 * Textos da interface nos três idiomas, embutidos no app para funcionar offline (spec, seção 8). O pt-BR é a
 * referência das chaves: faltar uma chave no es ou no en quebra a compilação, e a CI também confere chaves a mais.
 */
export type Traducoes = typeof ptBR;

/** Chave plana (sem aninhamento), no formato que vira uma linha por idioma se as traduções forem para o banco. */
export type ChaveDeTraducao = keyof Traducoes;

export const TRADUCOES: Record<Idioma, Traducoes> = {
  'pt-BR': ptBR,
  es,
  en,
};

export function ehChaveDeTraducao(texto: string): texto is ChaveDeTraducao {
  return Object.hasOwn(TRADUCOES['pt-BR'], texto);
}

/** Códigos da API e do cliente, em UPPER_SNAKE_CASE; as chaves auxiliares (erro.referencia) ficam em camelCase. */
const FORMATO_DE_CODIGO = /^[A-Z][A-Z0-9_]*$/;

/**
 * Chave da mensagem de um code do Problem Details ou do ErroDaApi. Código sem tradução (por exemplo, de um backend
 * mais novo que o app) cai na mensagem genérica, e nunca no title, que é só para log.
 */
export function chaveDoErro(code: string): ChaveDeTraducao {
  const chave = `erro.${code}`;
  return FORMATO_DE_CODIGO.test(code) && ehChaveDeTraducao(chave) ? chave : 'erro.desconhecido';
}

/**
 * Chave da mensagem de um item de errors. Regra de validação ainda sem código próprio (design da API, 1.6) cai na
 * mensagem genérica do campo.
 */
export function chaveDaValidacao(code: string): ChaveDeTraducao {
  const chave = `validacao.${code}`;
  return FORMATO_DE_CODIGO.test(code) && ehChaveDeTraducao(chave) ? chave : 'validacao.desconhecido';
}

/** Unidade sem tradução quebra a compilação aqui. */
export function chaveDaUnidade(unidade: Unidade): ChaveDeTraducao {
  return `dominio.unidade.${unidade}`;
}

/** Grandeza sem tradução quebra a compilação aqui. */
export function chaveDaGrandeza(grandeza: Grandeza): ChaveDeTraducao {
  return `dominio.grandeza.${grandeza}`;
}
