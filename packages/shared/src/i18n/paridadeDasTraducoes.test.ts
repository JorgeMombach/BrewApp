// Rodado sozinho pelo job de traduções da CI (pnpm verificar:traducoes), além de entrar no pnpm test
import { describe, expect, it } from 'vitest';
import { divergenciasDasTraducoes } from './paridadeDasTraducoes';
import { TRADUCOES } from './traducoes';

describe('traduções do app', () => {
  it('têm as mesmas chaves e variáveis nos três idiomas, sem texto vazio', () => {
    expect(divergenciasDasTraducoes(TRADUCOES)).toEqual([]);
  });
});

describe('divergenciasDasTraducoes', () => {
  it('aceita idiomas com as mesmas chaves e variáveis, em qualquer ordem', () => {
    expect(
      divergenciasDasTraducoes({
        'pt-BR': { 'app.a': 'Entre {{min}} e {{max, number}}', 'app.b': 'B' },
        es: { 'app.b': 'B', 'app.a': 'Entre {{max, number}} y {{min}}' },
        en: { 'app.a': 'Between {{min}} and {{max}}', 'app.b': 'B' },
      }),
    ).toEqual([]);
  });

  it('aponta chave faltando, a mais, vazia ou com variáveis diferentes', () => {
    expect(
      divergenciasDasTraducoes({
        'pt-BR': { 'app.a': 'A', 'app.b': 'Olá, {{nome}}', 'app.c': 'C' },
        es: { 'app.a': 'A', 'app.b': 'Hola', 'app.c': 'C', 'app.d': 'D' },
        en: { 'app.a': ' ', 'app.b': 'Hello, {{name}}' },
      }),
    ).toEqual([
      { idioma: 'es', chave: 'app.b', problema: 'VARIAVEIS_DIFERENTES' },
      { idioma: 'es', chave: 'app.d', problema: 'A_MAIS' },
      { idioma: 'en', chave: 'app.a', problema: 'VAZIA' },
      { idioma: 'en', chave: 'app.b', problema: 'VARIAVEIS_DIFERENTES' },
      { idioma: 'en', chave: 'app.c', problema: 'FALTANDO' },
    ]);
  });
});
