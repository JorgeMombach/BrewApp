import { act, fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { criarAutenticacaoDeTeste } from '../testes/autenticacaoDeTeste';
import { renderizarRotas } from '../testes/renderizarRotas';

async function renderizarComSessaoAnonima() {
  const autenticacaoDeTeste = criarAutenticacaoDeTeste();
  renderizarRotas('/', autenticacaoDeTeste.autenticacao);
  await act(() => autenticacaoDeTeste.autenticacao.gerenciadorDeSessao.iniciar());
  return autenticacaoDeTeste;
}

describe('botão de sessão', () => {
  it('não aparece enquanto a sessão está sendo restaurada', () => {
    renderizarRotas('/');

    expect(screen.queryByRole('button', { name: 'Entrar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Sair' })).not.toBeInTheDocument();
  });

  it('entra: avisa que o login segue no navegador e troca para Sair ao concluir', async () => {
    const { concluirEntrada } = await renderizarComSessaoAnonima();

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(screen.getByRole('status')).toHaveTextContent('Continue o login no navegador.');

    act(() => {
      concluirEntrada();
    });

    expect(await screen.findByRole('button', { name: 'Sair' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('avisa a falha e deixa tentar de novo', async () => {
    const { falharEntrada } = await renderizarComSessaoAnonima();
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
    act(() => {
      falharEntrada(new Error('navegador fechado'));
    });

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível entrar. Tente novamente.');
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled();
  });

  it('sai e volta a mostrar Entrar', async () => {
    const { concluirEntrada, sessoesEncerradas } = await renderizarComSessaoAnonima();
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
    act(() => {
      concluirEntrada();
    });

    fireEvent.click(await screen.findByRole('button', { name: 'Sair' }));

    expect(await screen.findByRole('button', { name: 'Entrar' })).toBeInTheDocument();
    expect(sessoesEncerradas).toHaveLength(1);
  });
});
