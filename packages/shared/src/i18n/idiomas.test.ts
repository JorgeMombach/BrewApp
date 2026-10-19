import { describe, expect, it } from 'vitest';
import { ehIdioma, idiomaPreferido } from './idiomas';

describe('idiomaPreferido', () => {
  it.each([
    { preferencias: ['pt-BR'], esperado: 'pt-BR' },
    { preferencias: ['es'], esperado: 'es' },
    { preferencias: ['en'], esperado: 'en' },
    { preferencias: ['pt-br'], esperado: 'pt-BR' },
    { preferencias: ['pt-PT'], esperado: 'pt-BR' },
    { preferencias: ['pt'], esperado: 'pt-BR' },
    { preferencias: ['es-MX'], esperado: 'es' },
    { preferencias: ['en-US'], esperado: 'en' },
  ])('$preferencias -> $esperado', ({ preferencias, esperado }) => {
    expect(idiomaPreferido(preferencias)).toBe(esperado);
  });

  it('pula preferências não suportadas e fica com a primeira suportada', () => {
    expect(idiomaPreferido(['fr-FR', 'de', 'es-AR', 'en'])).toBe('es');
  });

  it('cai no pt-BR quando nenhuma preferência é suportada', () => {
    expect(idiomaPreferido(['fr-FR', 'ja'])).toBe('pt-BR');
    expect(idiomaPreferido([])).toBe('pt-BR');
  });
});

describe('ehIdioma', () => {
  it('aceita só os códigos exatos da preferência do usuário', () => {
    expect(ehIdioma('pt-BR')).toBe(true);
    expect(ehIdioma('es')).toBe(true);
    expect(ehIdioma('en')).toBe(true);
    expect(ehIdioma('pt')).toBe(false);
    expect(ehIdioma('es-MX')).toBe(false);
  });
});
