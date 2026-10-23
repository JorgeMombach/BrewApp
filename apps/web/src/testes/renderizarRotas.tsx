import { render } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { ProvedorDeSessao } from '../autenticacao/ContextoDeSessao';
import type { AutenticacaoDaPlataforma } from '../autenticacao/plataforma';
import { rotas } from '../rotas';
import { criarAutenticacaoDeTeste } from './autenticacaoDeTeste';

/** Renderiza o app na rota pedida, com a mesma árvore do main.tsx; sem autenticação informada, a sessão é de teste. */
export function renderizarRotas(
  caminho: string,
  autenticacao: AutenticacaoDaPlataforma = criarAutenticacaoDeTeste().autenticacao,
) {
  const roteador = createMemoryRouter(rotas, { initialEntries: [caminho] });
  return render(
    <ProvedorDeSessao autenticacao={autenticacao}>
      <RouterProvider router={roteador} />
    </ProvedorDeSessao>,
  );
}
