import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { describe, expect, it } from 'vitest';
import { rotas } from './rotas';

function renderizarNaRota(caminho: string) {
  const roteador = createMemoryRouter(rotas, { initialEntries: [caminho] });
  render(<RouterProvider router={roteador} />);
}

describe('rotas', () => {
  it('abre a página inicial dentro do layout base', () => {
    renderizarNaRota('/');

    expect(screen.getByRole('navigation', { name: 'Navegação principal' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Diário de adega' })).toBeInTheDocument();
  });

  it('responde rota desconhecida com a página de não encontrada, com caminho de volta ao início', () => {
    renderizarNaRota('/rota-que-nao-existe');

    expect(screen.getByRole('heading', { level: 1, name: 'Página não encontrada' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Voltar ao início' })).toHaveAttribute('href', '/');
  });
});
