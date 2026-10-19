import { afterEach, describe, expect, it, vi } from 'vitest';
import { guardarIdiomaEscolhido, idiomaInicial } from './idiomaDoUsuario';

function simularIdiomasDoNavegador(idiomasDoNavegador: string[]) {
  vi.spyOn(navigator, 'languages', 'get').mockReturnValue(idiomasDoNavegador);
}

describe('idiomaInicial', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('usa as preferências do navegador quando não há escolha guardada', () => {
    simularIdiomasDoNavegador(['es-MX', 'en']);

    expect(idiomaInicial()).toBe('es');
  });

  it('prefere a escolha guardada às preferências do navegador', () => {
    simularIdiomasDoNavegador(['es-MX']);
    guardarIdiomaEscolhido('en');

    expect(idiomaInicial()).toBe('en');
  });

  it('ignora uma escolha guardada que não é idioma suportado', () => {
    simularIdiomasDoNavegador(['en-US']);
    localStorage.setItem('brewapp.idioma', 'fr');

    expect(idiomaInicial()).toBe('en');
  });

  it('cai no pt-BR quando o navegador não prefere nenhum idioma suportado', () => {
    simularIdiomasDoNavegador(['fr-FR', 'de']);

    expect(idiomaInicial()).toBe('pt-BR');
  });

  it('segue sem a escolha guardada quando o armazenamento do navegador está bloqueado', () => {
    simularIdiomasDoNavegador(['en']);
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Armazenamento bloqueado', 'SecurityError');
    });

    expect(idiomaInicial()).toBe('en');
  });
});
