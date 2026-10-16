import type { components } from './esquema.gerado';

/** Corpo de toda resposta de erro da API (design da API, seção 1.6). O cliente traduz o code, nunca exibe o title. */
export type ProblemDetails = components['schemas']['ProblemDetails'];

/** Item de ProblemDetails.errors nas falhas de validação. */
export type ErroDeValidacao = components['schemas']['ErroDeValidacao'];

function ehObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null && !Array.isArray(valor);
}

export function ehErroDeValidacao(valor: unknown): valor is ErroDeValidacao {
  return ehObjeto(valor) && typeof valor.campo === 'string' && typeof valor.code === 'string' && ehObjeto(valor.params);
}

/**
 * Confere em tempo de execução se um corpo de resposta é o Problem Details do BrewApp. O corpo chega como JSON sem
 * tipo; um erro de proxy ou de gateway, por exemplo, não segue o formato e não pode ser lido como se seguisse.
 */
export function ehProblemDetails(valor: unknown): valor is ProblemDetails {
  return (
    ehObjeto(valor) &&
    typeof valor.type === 'string' &&
    typeof valor.title === 'string' &&
    typeof valor.status === 'number' &&
    typeof valor.code === 'string' &&
    typeof valor.correlationId === 'string' &&
    ehObjeto(valor.params) &&
    Array.isArray(valor.errors) &&
    valor.errors.every(ehErroDeValidacao)
  );
}
