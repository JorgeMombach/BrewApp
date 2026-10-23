import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate } from 'react-router';
import { useSessao } from './ContextoDeSessao';
import { descreverFalha } from './descreverFalha';

/** Volta do Keycloak na web (/auth/callback): troca o code pelos tokens e leva o usuário de volta à página onde estava. */
export function PaginaDeRetornoDoLogin() {
  const { t } = useTranslation();
  const { concluirEntradaPorRedirect } = useSessao();
  const { pathname, search } = useLocation();
  const navegar = useNavigate();
  const [falhou, setFalhou] = useState(false);

  useEffect(() => {
    // O desktop não usa esta rota: o retorno dele chega no servidor de loopback
    if (concluirEntradaPorRedirect === undefined) {
      void navegar('/', { replace: true });
      return;
    }
    // O StrictMode roda o efeito duas vezes; o retorno é o mesmo nas duas, e só a última atualiza a tela
    let efeitoAtivo = true;
    concluirEntradaPorRedirect(`${window.location.origin}${pathname}${search}`)
      .then((destino) => {
        if (efeitoAtivo) {
          // replace: voltar no navegador não repete o retorno, que já não vale
          void navegar(destino, { replace: true });
        }
      })
      .catch((erro: unknown) => {
        console.error('Falha ao concluir o login', descreverFalha(erro));
        if (efeitoAtivo) {
          setFalhou(true);
        }
      });
    return () => {
      efeitoAtivo = false;
    };
  }, [concluirEntradaPorRedirect, navegar, pathname, search]);

  if (falhou) {
    return (
      <section>
        <h1>{t('app.sessao.falhaAoConcluirLogin')}</h1>
        <Link to="/">{t('app.sessao.voltarAoInicio')}</Link>
      </section>
    );
  }
  return <p role="status">{t('app.sessao.concluindoLogin')}</p>;
}
