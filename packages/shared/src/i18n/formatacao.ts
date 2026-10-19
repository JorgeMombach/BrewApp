import type { Idioma } from './idiomas';

/**
 * Número no formato do idioma (vírgula ou ponto decimal, separador de milhar). Para preencher um campo de entrada,
 * passar useGrouping: false: o lerNumeroDigitado não aceita separador de milhar.
 */
export function formatarNumero(valor: number, idioma: Idioma, opcoes?: Intl.NumberFormatOptions): string {
  return new Intl.NumberFormat(idioma, opcoes).format(valor);
}

/** Data no formato do idioma. Sem fuso, usa o do aparelho; com a preferência fusoHorario do GET /me, usa a dela. */
export function formatarData(instante: Date, idioma: Idioma, fusoHorario?: string): string {
  return new Intl.DateTimeFormat(idioma, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: fusoHorario,
  }).format(instante);
}

/** Data e hora (sem segundos) no formato do idioma, com o mesmo fuso do formatarData. */
export function formatarDataHora(instante: Date, idioma: Idioma, fusoHorario?: string): string {
  return new Intl.DateTimeFormat(idioma, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: fusoHorario,
  }).format(instante);
}

/** Sinal opcional, parte inteira e, se houver, um único separador decimal (vírgula ou ponto) seguido de dígitos. */
const NUMERO_DIGITADO = /^-?\d+(?:[.,]\d+)?$/;

/**
 * Lê o número digitado num campo, em qualquer idioma: vírgula e ponto valem como separador decimal, porque a mesma
 * densidade chega como 1,050 de quem usa vírgula e 1.050 de quem usa ponto (no México, por exemplo, o espanhol usa
 * ponto). Separador de milhar não é aceito, porque deixaria 1.050 ambíguo. Texto fora do formato devolve null.
 */
export function lerNumeroDigitado(texto: string): number | null {
  const textoSemEspacos = texto.trim();
  if (!NUMERO_DIGITADO.test(textoSemEspacos)) {
    return null;
  }
  return Number(textoSemEspacos.replace(',', '.'));
}
