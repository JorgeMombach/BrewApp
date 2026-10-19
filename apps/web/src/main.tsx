import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { iniciarI18n } from './i18n/configuracaoI18n';
import { idiomaInicial } from './i18n/idiomaDoUsuario';
import { rotas } from './rotas';
import './estilos.css';

iniciarI18n(idiomaInicial());

const elementoRaiz = document.getElementById('raiz');
if (elementoRaiz === null) {
  throw new Error('Elemento #raiz não encontrado no index.html');
}

const roteador = createBrowserRouter(rotas);

createRoot(elementoRaiz).render(
  <StrictMode>
    <RouterProvider router={roteador} />
  </StrictMode>,
);
