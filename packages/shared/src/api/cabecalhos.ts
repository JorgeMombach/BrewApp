/** Cabeçalhos próprios do BrewApp (design da API, seção 1.2), os mesmos do CabecalhosBrewApp do backend. */
export const CabecalhosBrewApp = {
  /** Organização em uso; a única origem do tenant numa requisição */
  X_ORGANIZATION_ID: 'X-Organization-Id',
  /** Volta em toda resposta e no correlationId dos erros */
  X_CORRELATION_ID: 'X-Correlation-Id',
} as const;
