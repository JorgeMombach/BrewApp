// Tipos do contrato da API, gerados a partir do OpenAPI exportado pelo backend (pnpm gerar:tipos)
export type { components, paths } from './api/esquema.gerado';
export type { ProblemDetails, ErroDeValidacao } from './api/erros';
export { ehErroDeValidacao, ehProblemDetails } from './api/erros';
export { CabecalhosBrewApp } from './api/cabecalhos';

// Catálogo de unidades compartilhado com o backend, testado contra os mesmos casos (casos-de-teste/)
export {
  GRANDEZAS,
  UNIDADES,
  converter,
  ehCanonica,
  ehGrandeza,
  ehUnidade,
  ehUnidadeDaGrandeza,
  grandezaDe,
  unidadeCanonica,
} from './unidades/catalogo';
export type { Grandeza, Unidade } from './unidades/catalogo';

// Internacionalização (spec, seção 8): traduções embutidas, idiomas e formatação de números e datas com Intl
export { IDIOMAS, IDIOMA_PADRAO, ehIdioma, idiomaPreferido } from './i18n/idiomas';
export type { Idioma } from './i18n/idiomas';
export {
  TRADUCOES,
  chaveDaGrandeza,
  chaveDaUnidade,
  chaveDaValidacao,
  chaveDoErro,
  ehChaveDeTraducao,
} from './i18n/traducoes';
export type { ChaveDeTraducao, Traducoes } from './i18n/traducoes';
export { formatarData, formatarDataHora, formatarNumero, lerNumeroDigitado } from './i18n/formatacao';
