/**
 * Cópia em TypeScript do catálogo de unidades do backend (Unidade.java, a fonte da verdade; design da API, seção
 * 1.11), para pré-visualização e validação no aparelho, inclusive sem rede. As duas são testadas contra os mesmos
 * arquivos de casos-de-teste/ (spec 4.5). Incluir uma unidade é acrescentar uma entrada aqui e no backend, com os
 * seus casos; a chave é o código usado na API.
 */
export const GRANDEZAS = ['DENSIDADE', 'TEMPERATURA', 'PRESSAO', 'VOLUME', 'MASSA'] as const;

export type Grandeza = (typeof GRANDEZAS)[number];

export const UNIDADES = [
  'SG',
  'PLATO',
  'BRIX',
  'G_CM3',
  'CELSIUS',
  'FAHRENHEIT',
  'BAR',
  'PSI',
  'KPA',
  'L',
  'HL',
  'ML',
  'GAL_US',
  'GAL_UK',
  'BBL_US',
  'G',
  'KG',
  'OZ',
  'LB',
] as const;

export type Unidade = (typeof UNIDADES)[number];

/** Conversão de uma unidade para a canônica da sua grandeza e de volta. */
interface Conversao {
  paraCanonica(valor: number): number;
  daCanonica(valorCanonico: number): number;
}

/** Com deslocamento de zero, como o °F: canônica = (valor − zero) × fator. */
function afim(fator: number, zero: number): Conversao {
  return {
    paraCanonica: (valor) => (valor - zero) * fator,
    daCanonica: (valorCanonico) => valorCanonico / fator + zero,
  };
}

/** Proporcional: canônica = valor × fator. */
function linear(fator: number): Conversao {
  return afim(fator, 0);
}

/** Da própria unidade canônica: não converte. */
const CANONICA = afim(1, 0);

/**
 * Escala de sacarose (graus Plato e Brix): porcentagem em massa de extrato. As duas escalas vêm da mesma tabela de
 * sacarose e diferem menos que a precisão de um densímetro; as fórmulas são aproximações da tabela da ASBC.
 */
const ESCALA_DE_SACAROSE: Conversao = {
  paraCanonica: (grausDeExtrato) => 1 + grausDeExtrato / (258.6 - (grausDeExtrato / 258.2) * 227.1),
  daCanonica: (gravidadeEspecifica) =>
    -616.868 +
    1111.14 * gravidadeEspecifica -
    630.272 * gravidadeEspecifica ** 2 +
    135.997 * gravidadeEspecifica ** 3,
};

interface EntradaDoCatalogo {
  grandeza: Grandeza;
  conversao: Conversao;
}

const CATALOGO: Record<Unidade, EntradaDoCatalogo> = {
  SG: { grandeza: 'DENSIDADE', conversao: CANONICA },
  PLATO: { grandeza: 'DENSIDADE', conversao: ESCALA_DE_SACAROSE },
  // Escala de sacarose, como o Plato. A leitura de refratômetro com álcool no mosto exige correção fora do catálogo
  BRIX: { grandeza: 'DENSIDADE', conversao: ESCALA_DE_SACAROSE },
  // Densidade absoluta, relativa à da água a 20 °C (0,9982067 g/cm³; Tanaka et al., 2001)
  G_CM3: { grandeza: 'DENSIDADE', conversao: linear(1 / 0.9982067) },

  CELSIUS: { grandeza: 'TEMPERATURA', conversao: CANONICA },
  FAHRENHEIT: { grandeza: 'TEMPERATURA', conversao: afim(1 / 1.8, 32) },

  BAR: { grandeza: 'PRESSAO', conversao: CANONICA },
  // Libra-força por polegada quadrada: 0,45359237 kg × 9,80665 m/s² / (0,0254 m)², em bar
  PSI: { grandeza: 'PRESSAO', conversao: linear((0.45359237 * 9.80665) / (0.0254 * 0.0254) / 100_000) },
  KPA: { grandeza: 'PRESSAO', conversao: linear(0.01) },

  L: { grandeza: 'VOLUME', conversao: CANONICA },
  HL: { grandeza: 'VOLUME', conversao: linear(100) },
  ML: { grandeza: 'VOLUME', conversao: linear(0.001) },
  GAL_US: { grandeza: 'VOLUME', conversao: linear(3.785411784) },
  GAL_UK: { grandeza: 'VOLUME', conversao: linear(4.54609) },
  // Barril de cerveja americano: 31 galões US
  BBL_US: { grandeza: 'VOLUME', conversao: linear(31 * 3.785411784) },

  G: { grandeza: 'MASSA', conversao: CANONICA },
  KG: { grandeza: 'MASSA', conversao: linear(1000) },
  // Onça avoirdupois: 1/16 de libra
  OZ: { grandeza: 'MASSA', conversao: linear(453.59237 / 16) },
  LB: { grandeza: 'MASSA', conversao: linear(453.59237) },
};

export function ehUnidade(codigo: string): codigo is Unidade {
  return UNIDADES.some((unidade) => unidade === codigo);
}

export function ehGrandeza(codigo: string): codigo is Grandeza {
  return GRANDEZAS.some((grandeza) => grandeza === codigo);
}

export function grandezaDe(unidade: Unidade): Grandeza {
  return CATALOGO[unidade].grandeza;
}

/**
 * Validação de campo: o código existe no catálogo e é da grandeza esperada (ex.: gramas não servem para volume).
 * No backend, a mesma regra rejeita a Quantidade com UNIDADE_INVALIDA.
 */
export function ehUnidadeDaGrandeza(codigo: string, grandeza: Grandeza): codigo is Unidade {
  return ehUnidade(codigo) && grandezaDe(codigo) === grandeza;
}

export function ehCanonica(unidade: Unidade): boolean {
  return CATALOGO[unidade].conversao === CANONICA;
}

/** A unidade em que o valor canônico é guardado e comparado. */
export function unidadeCanonica(grandeza: Grandeza): Unidade {
  const canonica = UNIDADES.find((unidade) => grandezaDe(unidade) === grandeza && ehCanonica(unidade));
  if (canonica === undefined) {
    throw new Error(`Grandeza sem unidade canônica no catálogo: ${grandeza}`);
  }
  return canonica;
}

/**
 * Converte passando pela canônica. Na mesma unidade devolve o valor sem conversão: as fórmulas de ida e volta da
 * escala de sacarose não são inversas exatas, e 12 °P não pode virar 12,007 °P.
 */
export function converter(valor: number, origem: Unidade, destino: Unidade): number {
  if (grandezaDe(origem) !== grandezaDe(destino)) {
    throw new Error(
      `Conversão entre grandezas diferentes: ${origem} (${grandezaDe(origem)}) para ${destino} (${grandezaDe(destino)})`,
    );
  }
  if (origem === destino) {
    return valor;
  }
  return CATALOGO[destino].conversao.daCanonica(CATALOGO[origem].conversao.paraCanonica(valor));
}
