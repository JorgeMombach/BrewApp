import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Outlet } from 'react-router';
import { BotaoDeSessao } from '../autenticacao/BotaoDeSessao';
import { SeletorDeIdioma } from '../i18n/SeletorDeIdioma';

/** Moldura comum a todas as páginas: cabeçalho com a navegação, a sessão e o idioma, e a página da rota atual. */
export function LayoutBase() {
  const { t, i18n } = useTranslation();

  // O <html lang> acompanha o idioma em uso, para o leitor de tela pronunciar e o navegador hifenizar certo
  useEffect(() => {
    document.documentElement.lang = i18n.language;
  }, [i18n.language]);

  return (
    <>
      <header className="cabecalho">
        <nav aria-label={t('app.layout.navegacaoPrincipal')}>
          <Link to="/" className="marca">
            {t('app.nome')}
          </Link>
        </nav>
        <div className="acoes-do-cabecalho">
          <BotaoDeSessao />
          <SeletorDeIdioma />
        </div>
      </header>
      <main className="conteudo">
        <Outlet />
      </main>
    </>
  );
}
