import { describe, expect, it } from 'vitest';
import { formatarData, formatarDataHora, formatarNumero, lerNumeroDigitado } from './formatacao';

describe('lerNumeroDigitado (entrada de densidade)', () => {
  it.each([
    { texto: '1,050', esperado: 1.05 },
    { texto: '1.050', esperado: 1.05 },
    { texto: '12,5', esperado: 12.5 },
    { texto: '12.5', esperado: 12.5 },
    { texto: '1', esperado: 1 },
    { texto: ' 1,048 ', esperado: 1.048 },
    { texto: '-2,5', esperado: -2.5 },
  ])('"$texto" vale $esperado em qualquer idioma', ({ texto, esperado }) => {
    expect(lerNumeroDigitado(texto)).toBe(esperado);
  });

  it.each([
    { texto: '', motivo: 'vazio' },
    { texto: '   ', motivo: 'só espaços' },
    { texto: '1.050,5', motivo: 'separador de milhar e decimal juntos' },
    { texto: '1,050.5', motivo: 'separador de milhar e decimal juntos' },
    { texto: '1.000.000', motivo: 'separador de milhar' },
    { texto: '1,', motivo: 'separador sem casas decimais' },
    { texto: ',5', motivo: 'separador sem parte inteira' },
    { texto: '1 050', motivo: 'espaço no meio' },
    { texto: '1e3', motivo: 'notação científica' },
    { texto: 'abc', motivo: 'texto' },
  ])('"$texto" é inválido ($motivo)', ({ texto }) => {
    expect(lerNumeroDigitado(texto)).toBeNull();
  });

  it('lê de volta o valor formatado para um campo de entrada, em todos os idiomas', () => {
    const opcoesDoCampo: Intl.NumberFormatOptions = { minimumFractionDigits: 3, useGrouping: false };

    expect(lerNumeroDigitado(formatarNumero(1.05, 'pt-BR', opcoesDoCampo))).toBe(1.05);
    expect(lerNumeroDigitado(formatarNumero(1.05, 'es', opcoesDoCampo))).toBe(1.05);
    expect(lerNumeroDigitado(formatarNumero(1.05, 'en', opcoesDoCampo))).toBe(1.05);
  });
});

describe('formatarNumero', () => {
  it('usa o separador decimal do idioma', () => {
    const tresCasas: Intl.NumberFormatOptions = { minimumFractionDigits: 3 };

    expect(formatarNumero(1.05, 'pt-BR', tresCasas)).toBe('1,050');
    expect(formatarNumero(1.05, 'es', tresCasas)).toBe('1,050');
    expect(formatarNumero(1.05, 'en', tresCasas)).toBe('1.050');
  });

  it('usa o separador de milhar do idioma', () => {
    expect(formatarNumero(12345.5, 'pt-BR')).toBe('12.345,5');
    expect(formatarNumero(12345.5, 'es')).toBe('12.345,5');
    expect(formatarNumero(12345.5, 'en')).toBe('12,345.5');
  });
});

describe('formatarData e formatarDataHora', () => {
  const instante = new Date('2026-10-19T13:05:00Z');

  it('seguem a ordem de dia e mês do idioma', () => {
    expect(formatarData(instante, 'pt-BR', 'America/Sao_Paulo')).toBe('19/10/2026');
    expect(formatarData(instante, 'es', 'America/Sao_Paulo')).toBe('19/10/2026');
    expect(formatarData(instante, 'en', 'America/Sao_Paulo')).toBe('10/19/2026');
  });

  it('mostram a hora no fuso pedido', () => {
    expect(formatarDataHora(instante, 'pt-BR', 'America/Sao_Paulo')).toBe('19/10/2026, 10:05');
    expect(formatarDataHora(instante, 'es', 'America/Mexico_City')).toBe('19/10/2026, 07:05');
    // O Intl separa o AM com espaço estreito (U+202F) nas versões recentes do ICU
    expect(formatarDataHora(instante, 'en', 'America/Sao_Paulo')).toMatch(/^10\/19\/2026, 10:05\sAM$/);
  });
});
