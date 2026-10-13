import type { components } from './esquema.gerado';

/** Corpo de toda resposta de erro da API (design da API, seção 1.6). O cliente traduz o code, nunca exibe o title. */
export type ProblemDetails = components['schemas']['ProblemDetails'];

/** Item de ProblemDetails.errors nas falhas de validação. */
export type ErroDeValidacao = components['schemas']['ErroDeValidacao'];
