import { useTranslation } from 'react-i18next';

export function PaginaInicial() {
  const { t } = useTranslation();

  return (
    <section>
      <h1>{t('app.paginaInicial.titulo')}</h1>
      <p>{t('app.paginaInicial.descricao')}</p>
    </section>
  );
}
