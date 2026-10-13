import { describe, expect, it } from 'vitest';
import casosDeDensidade from '../../casos-de-teste/densidade.json';
import type { ArquivoDeCasos } from '../casos-de-teste/arquivo-de-casos';
import { converterDensidade, ehUnidadeDeDensidade, type UnidadeDeDensidade } from './densidade';

// Mesmos casos do teste Java (UnidadeDeDensidadeTest), lidos do mesmo arquivo
const arquivoDeCasos: ArquivoDeCasos = casosDeDensidade;

function unidadeDoCaso(unidade: string): UnidadeDeDensidade {
  if (!ehUnidadeDeDensidade(unidade)) {
    throw new Error(`Unidade de densidade desconhecida no arquivo de casos: ${unidade}`);
  }
  return unidade;
}

describe('conversão de densidade', () => {
  it.each(arquivoDeCasos.casos)('$descricao: $de.valor $de.unidade -> $para', (casoDeConversao) => {
    const valorConvertido = converterDensidade(
      casoDeConversao.de.valor,
      unidadeDoCaso(casoDeConversao.de.unidade),
      unidadeDoCaso(casoDeConversao.para),
    );

    expect(Math.abs(valorConvertido - casoDeConversao.esperado)).toBeLessThanOrEqual(casoDeConversao.tolerancia);
  });

  it('reconhece só as unidades do catálogo', () => {
    expect(ehUnidadeDeDensidade('PLATO')).toBe(true);
    expect(ehUnidadeDeDensidade('plato')).toBe(false);
  });
});
