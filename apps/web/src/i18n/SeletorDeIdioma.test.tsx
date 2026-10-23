import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { renderizarRotas } from '../testes/renderizarRotas';
import { idiomaInicial } from './idiomaDoUsuario';

function renderizarPaginaInicial() {
  renderizarRotas('/');
}

describe('seletor de idioma', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('mostra cada idioma pelo nome na própria língua, com o atual selecionado', () => {
    renderizarPaginaInicial();

    const seletor = screen.getByRole('combobox', { name: 'Idioma' });
    expect(seletor).toHaveValue('pt-BR');
    expect(screen.getAllByRole('option').map((opcao) => opcao.textContent)).toEqual([
      'Português (Brasil)',
      'Español',
      'English',
    ]);
  });

  it('troca os textos da tela e o lang do documento ao escolher outro idioma', async () => {
    renderizarPaginaInicial();

    fireEvent.change(screen.getByRole('combobox', { name: 'Idioma' }), { target: { value: 'en' } });

    expect(await screen.findByRole('heading', { level: 1, name: 'Cellar log' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Language' })).toHaveValue('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it('guarda a escolha para a próxima abertura do app', () => {
    renderizarPaginaInicial();

    fireEvent.change(screen.getByRole('combobox', { name: 'Idioma' }), { target: { value: 'es' } });

    expect(idiomaInicial()).toBe('es');
  });
});
