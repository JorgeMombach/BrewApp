import { IDIOMAS, IDIOMA_PADRAO, ehIdioma, type Idioma } from '@brewapp/shared';
import type { ChangeEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { guardarIdiomaEscolhido } from './idiomaDoUsuario';

/**
 * Nome do idioma na própria língua (Português, Español, English), como é costume em seletor de idioma: quem não lê o
 * idioma atual ainda encontra o seu. Vem do Intl, por isso não tem chave de tradução.
 */
function nomeDoIdioma(idioma: Idioma): string {
  const nome = new Intl.DisplayNames(idioma, { type: 'language' }).of(idioma) ?? idioma;
  return nome.charAt(0).toLocaleUpperCase(idioma) + nome.slice(1);
}

export function SeletorDeIdioma() {
  const { t, i18n } = useTranslation();
  const idiomaAtual = ehIdioma(i18n.language) ? i18n.language : IDIOMA_PADRAO;

  function trocarIdioma(evento: ChangeEvent<HTMLSelectElement>) {
    const idiomaEscolhido = evento.target.value;
    if (!ehIdioma(idiomaEscolhido)) {
      return;
    }
    guardarIdiomaEscolhido(idiomaEscolhido);
    void i18n.changeLanguage(idiomaEscolhido);
  }

  return (
    <select aria-label={t('app.layout.idioma')} value={idiomaAtual} onChange={trocarIdioma}>
      {IDIOMAS.map((idioma) => (
        <option key={idioma} value={idioma} lang={idioma}>
          {nomeDoIdioma(idioma)}
        </option>
      ))}
    </select>
  );
}
