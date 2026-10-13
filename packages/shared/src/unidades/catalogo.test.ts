import { describe, expect, it } from 'vitest';
import casosDeDensidade from '../../casos-de-teste/densidade.json';
import casosDeMassa from '../../casos-de-teste/massa.json';
import casosDePressao from '../../casos-de-teste/pressao.json';
import casosDeTemperatura from '../../casos-de-teste/temperatura.json';
import casosDeVolume from '../../casos-de-teste/volume.json';
import type { ArquivoDeCasos, CasoDeConversao } from '../casos-de-teste/arquivo-de-casos';
import {
  GRANDEZAS,
  UNIDADES,
  converter,
  ehGrandeza,
  ehUnidade,
  ehUnidadeDaGrandeza,
  grandezaDe,
  unidadeCanonica,
  type Grandeza,
  type Unidade,
} from './catalogo';

// Mesmos casos do teste Java (CatalogoDeUnidadesTest), lidos dos mesmos arquivos; um arquivo por grandeza
const arquivosDeCasos: ArquivoDeCasos[] = [
  casosDeDensidade,
  casosDeTemperatura,
  casosDePressao,
  casosDeVolume,
  casosDeMassa,
];

const casosCompartilhados = arquivosDeCasos.flatMap((arquivoDeCasos) => arquivoDeCasos.casos);

function unidadeDoCaso(codigo: string): Unidade {
  if (!ehUnidade(codigo)) {
    throw new Error(`Unidade desconhecida no arquivo de casos: ${codigo}`);
  }
  return codigo;
}

function grandezaDoArquivo(codigo: string): Grandeza {
  if (!ehGrandeza(codigo)) {
    throw new Error(`Grandeza desconhecida no arquivo de casos: ${codigo}`);
  }
  return codigo;
}

function unidadesDosCasos(casosDeConversao: CasoDeConversao[]): Unidade[] {
  return casosDeConversao.flatMap((casoDeConversao) => [
    unidadeDoCaso(casoDeConversao.de.unidade),
    unidadeDoCaso(casoDeConversao.para),
  ]);
}

describe('catálogo de unidades', () => {
  it.each(casosCompartilhados)('$descricao: $de.valor $de.unidade -> $para', (casoDeConversao) => {
    const valorConvertido = converter(
      casoDeConversao.de.valor,
      unidadeDoCaso(casoDeConversao.de.unidade),
      unidadeDoCaso(casoDeConversao.para),
    );

    expect(Math.abs(valorConvertido - casoDeConversao.esperado)).toBeLessThanOrEqual(casoDeConversao.tolerancia);
  });

  // Incluir uma unidade no catálogo exige incluir os seus casos (critério da BREW-45)
  it('toda unidade do catálogo tem casos compartilhados', () => {
    const unidadesComCasos = new Set(unidadesDosCasos(casosCompartilhados));

    expect([...unidadesComCasos].sort()).toEqual([...UNIDADES].sort());
  });

  it.each(arquivosDeCasos)('casos do arquivo de $grandeza são dessa grandeza', (arquivoDeCasos) => {
    const grandeza = grandezaDoArquivo(arquivoDeCasos.grandeza);

    for (const unidade of unidadesDosCasos(arquivoDeCasos.casos)) {
      expect(grandezaDe(unidade)).toBe(grandeza);
    }
  });

  it('cada grandeza tem a unidade canônica do design da API', () => {
    const canonicaPorGrandeza = Object.fromEntries(GRANDEZAS.map((grandeza) => [grandeza, unidadeCanonica(grandeza)]));

    expect(canonicaPorGrandeza).toEqual({
      DENSIDADE: 'SG',
      TEMPERATURA: 'CELSIUS',
      PRESSAO: 'BAR',
      VOLUME: 'L',
      MASSA: 'G',
    });
  });

  it('não converte entre grandezas diferentes', () => {
    expect(() => converter(1.05, 'SG', 'L')).toThrow('Conversão entre grandezas diferentes: SG (DENSIDADE) para L (VOLUME)');
  });

  it('reconhece só os códigos do catálogo', () => {
    expect(ehUnidade('PLATO')).toBe(true);
    expect(ehUnidade('plato')).toBe(false);
  });

  it('rejeita unidade de outra grandeza', () => {
    expect(ehUnidadeDaGrandeza('L', 'VOLUME')).toBe(true);
    expect(ehUnidadeDaGrandeza('G', 'VOLUME')).toBe(false);
    expect(ehUnidadeDaGrandeza('LITRO', 'VOLUME')).toBe(false);
  });
});
