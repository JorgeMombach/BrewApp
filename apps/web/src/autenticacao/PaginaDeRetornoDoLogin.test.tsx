import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { criarAutenticacaoDeTeste } from '../testes/autenticacaoDeTeste';
import { renderizarRotas } from '../testes/renderizarRotas';

describe('página de retorno do login', () => {
  it('conclui o login com a URL do retorno e leva o usuário de volta à página onde estava', async () => {
    const concluirEntradaPorRedirect = vi.fn(() => Promise.resolve('/rota-de-origem'));
    const { autenticacao } = criarAutenticacaoDeTeste({ concluirEntradaPorRedirect, abreNavegadorExterno: false });

    renderizarRotas('/auth/callback?code=abc&state=xyz', autenticacao);

    expect(screen.getByRole('status')).toHaveTextContent('Concluindo o login…');
    // /rota-de-origem não existe nas rotas de teste: chegar na página de não encontrada prova a navegação
    expect(await screen.findByRole('heading', { level: 1, name: 'Página não encontrada' })).toBeInTheDocument();
    expect(concluirEntradaPorRedirect).toHaveBeenCalledWith(
      `${window.location.origin}/auth/callback?code=abc&state=xyz`,
    );
  });

  it('avisa a falha e oferece a volta ao início', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { autenticacao } = criarAutenticacaoDeTeste({
      concluirEntradaPorRedirect: () => Promise.reject(new Error('state desconhecido')),
      abreNavegadorExterno: false,
    });

    renderizarRotas('/auth/callback?code=abc&state=forjado', autenticacao);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Não foi possível concluir o login.' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Voltar ao início' })).toHaveAttribute('href', '/');
  });

  it('no desktop, que não usa esta rota, volta para o início', async () => {
    renderizarRotas('/auth/callback?code=abc&state=xyz');

    expect(await screen.findByRole('heading', { level: 1, name: 'Diário de adega' })).toBeInTheDocument();
  });
});
