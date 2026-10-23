import { useTranslation } from 'react-i18next';
import { useSessao } from './ContextoDeSessao';

/** Entrar ou sair, conforme a sessão; nada enquanto ela ainda está sendo restaurada. */
export function BotaoDeSessao() {
  const { t } = useTranslation();
  const { situacao, entrando, falhaAoEntrar, abreNavegadorExterno, entrar, sair } = useSessao();

  if (situacao === 'iniciando') {
    return null;
  }
  if (situacao === 'autenticada') {
    return (
      <button type="button" onClick={sair}>
        {t('app.sessao.sair')}
      </button>
    );
  }
  return (
    <div className="sessao">
      {entrando && abreNavegadorExterno && <span role="status">{t('app.sessao.continueNoNavegador')}</span>}
      {falhaAoEntrar && <span role="alert">{t('app.sessao.falhaAoEntrar')}</span>}
      <button type="button" onClick={entrar}>
        {t('app.sessao.entrar')}
      </button>
    </div>
  );
}
