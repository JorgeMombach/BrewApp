import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';

export function PaginaNaoEncontrada() {
  const { t } = useTranslation();

  return (
    <section>
      <h1>{t('app.paginaNaoEncontrada.titulo')}</h1>
      <Link to="/">{t('app.paginaNaoEncontrada.voltarAoInicio')}</Link>
    </section>
  );
}
