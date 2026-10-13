// Tipos do contrato da API, gerados a partir do OpenAPI exportado pelo backend (pnpm gerar:tipos)
export type { components, paths } from './api/esquema.gerado';
export type { ProblemDetails, ErroDeValidacao } from './api/erros';

// Conversões compartilhadas com o backend, testadas contra os mesmos casos (casos-de-teste/)
export { UNIDADES_DE_DENSIDADE, converterDensidade, ehUnidadeDeDensidade } from './unidades/densidade';
export type { UnidadeDeDensidade } from './unidades/densidade';
