import { Link, Outlet } from 'react-router';
import { textos } from '../textos';

/** Moldura comum a todas as páginas: cabeçalho com a navegação e a página da rota atual. */
export function LayoutBase() {
  return (
    <>
      <header className="cabecalho">
        <nav aria-label={textos.navegacaoPrincipal}>
          <Link to="/" className="marca">
            {textos.nomeDoApp}
          </Link>
        </nav>
      </header>
      <main className="conteudo">
        <Outlet />
      </main>
    </>
  );
}
