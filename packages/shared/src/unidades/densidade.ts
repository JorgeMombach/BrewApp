/**
 * Cópia em TypeScript das conversões de densidade do backend (UnidadeDeDensidade.java, a fonte da verdade), para
 * pré-visualização no aparelho, inclusive sem rede. As duas são testadas contra casos-de-teste/densidade.json (spec 4.5).
 * Exemplo da BREW-18; o catálogo completo vem na BREW-45.
 */
export const UNIDADES_DE_DENSIDADE = ['SG', 'PLATO'] as const;

export type UnidadeDeDensidade = (typeof UNIDADES_DE_DENSIDADE)[number];

interface ConversaoParaCanonica {
  paraSg(valor: number): number;
  deSg(gravidadeEspecifica: number): number;
}

const conversoes: Record<UnidadeDeDensidade, ConversaoParaCanonica> = {
  SG: {
    paraSg: (valor) => valor,
    deSg: (gravidadeEspecifica) => gravidadeEspecifica,
  },
  // Graus Plato: porcentagem em massa de extrato. As duas fórmulas são aproximações da tabela da ASBC
  PLATO: {
    paraSg: (grausPlato) => 1 + grausPlato / (258.6 - (grausPlato / 258.2) * 227.1),
    deSg: (gravidadeEspecifica) =>
      -616.868 +
      1111.14 * gravidadeEspecifica -
      630.272 * gravidadeEspecifica ** 2 +
      135.997 * gravidadeEspecifica ** 3,
  },
};

export function ehUnidadeDeDensidade(unidade: string): unidade is UnidadeDeDensidade {
  return UNIDADES_DE_DENSIDADE.some((unidadeDoCatalogo) => unidadeDoCatalogo === unidade);
}

/**
 * Converte passando pela canônica (SG). Na mesma unidade devolve o valor sem conversão: as fórmulas de ida e volta do
 * Plato não são inversas exatas, e 12 °P não pode virar 12,007 °P.
 */
export function converterDensidade(valor: number, origem: UnidadeDeDensidade, destino: UnidadeDeDensidade): number {
  if (origem === destino) {
    return valor;
  }
  return conversoes[destino].deSg(conversoes[origem].paraSg(valor));
}
