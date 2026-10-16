import { describe, expect, it } from 'vitest';
import { ehProblemDetails } from './erros';

// Corpo do exemplo da seção 1.6 do design da API
const loteEnvasado = {
  type: 'https://brewapp.dev/erros/lote-envasado',
  title: 'Lote já envasado',
  status: 409,
  code: 'LOTE_ENVASADO',
  params: { loteId: '0192', envasadoEm: '2026-10-01T10:00:00Z' },
  correlationId: '4f1c',
  errors: [],
};

describe('ehProblemDetails', () => {
  it('aceita o corpo de erro do design da API', () => {
    expect(ehProblemDetails(loteEnvasado)).toBe(true);
  });

  it('aceita erro de validação com os campos em errors', () => {
    const validacaoFalhou = {
      ...loteEnvasado,
      status: 400,
      code: 'VALIDACAO_FALHOU',
      params: {},
      errors: [{ campo: 'capacidade.valor', code: 'DEVE_SER_POSITIVO', params: {} }],
    };

    expect(ehProblemDetails(validacaoFalhou)).toBe(true);
  });

  it('rejeita o ProblemDetail padrão do Spring, sem code nem correlationId', () => {
    const problemDetailDoSpring = { type: 'about:blank', title: 'Not Found', status: 404, instance: '/api/v1/x' };

    expect(ehProblemDetails(problemDetailDoSpring)).toBe(false);
  });

  it('rejeita item de errors fora do formato', () => {
    const itemSemCampo = { ...loteEnvasado, errors: [{ code: 'OBRIGATORIO', params: {} }] };

    expect(ehProblemDetails(itemSemCampo)).toBe(false);
  });

  it.each([null, 'Bad Gateway', 502, [loteEnvasado]])('rejeita corpo que não é objeto: %j', (corpo) => {
    expect(ehProblemDetails(corpo)).toBe(false);
  });
});
