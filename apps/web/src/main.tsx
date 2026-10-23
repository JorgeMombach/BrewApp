import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { lerConfiguracaoOidc } from './autenticacao/configuracaoOidc';
import { ProvedorDeSessao } from './autenticacao/ContextoDeSessao';
import { descreverFalha } from './autenticacao/descreverFalha';
import { criarAutenticacaoDaPlataforma } from './autenticacao/plataforma';
import { iniciarI18n } from './i18n/configuracaoI18n';
import { idiomaInicial } from './i18n/idiomaDoUsuario';
import { rotas } from './rotas';
import './estilos.css';

iniciarI18n(idiomaInicial());

const elementoRaiz = document.getElementById('raiz');
if (elementoRaiz === null) {
  throw new Error('Elemento #raiz não encontrado no index.html');
}

const autenticacao = criarAutenticacaoDaPlataforma(lerConfiguracaoOidc());
// Fora do React: o StrictMode montaria duas vezes, e a restauração da sessão deve rodar uma vez só
autenticacao.gerenciadorDeSessao.iniciar().catch((erro: unknown) => {
  console.error('Não foi possível restaurar a sessão', descreverFalha(erro));
});

const roteador = createBrowserRouter(rotas);

createRoot(elementoRaiz).render(
  <StrictMode>
    <ProvedorDeSessao autenticacao={autenticacao}>
      <RouterProvider router={roteador} />
    </ProvedorDeSessao>
  </StrictMode>,
);
