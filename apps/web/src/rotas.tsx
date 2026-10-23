import type { RouteObject } from 'react-router';
import { PaginaDeRetornoDoLogin } from './autenticacao/PaginaDeRetornoDoLogin';
import { LayoutBase } from './layout/LayoutBase';
import { PaginaInicial } from './paginas/PaginaInicial';
import { PaginaNaoEncontrada } from './paginas/PaginaNaoEncontrada';

/**
 * Rotas do app, separadas do roteador: o navegador usa o createBrowserRouter (main.tsx) e os testes, o
 * createMemoryRouter, com a mesma árvore. Toda página nova entra como filha do layout base.
 */
export const rotas: RouteObject[] = [
  {
    path: '/',
    element: <LayoutBase />,
    children: [
      { index: true, element: <PaginaInicial /> },
      // Redirect cadastrado no client brewapp-web do Keycloak (CAMINHO_DO_RETORNO_DO_LOGIN)
      { path: 'auth/callback', element: <PaginaDeRetornoDoLogin /> },
      { path: '*', element: <PaginaNaoEncontrada /> },
    ],
  },
];
