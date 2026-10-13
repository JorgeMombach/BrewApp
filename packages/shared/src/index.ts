// Tipos do contrato da API, gerados a partir do OpenAPI exportado pelo backend (pnpm gerar:tipos)
export type { components, paths } from './api/esquema.gerado';
export type { ProblemDetails, ErroDeValidacao } from './api/erros';

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
