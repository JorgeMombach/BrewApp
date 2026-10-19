import { IDIOMAS, IDIOMA_PADRAO, TRADUCOES, type Idioma } from '@brewapp/shared';
import i18next, { type Resource } from 'i18next';
import { initReactI18next } from 'react-i18next';

const recursos: Resource = Object.fromEntries(IDIOMAS.map((idioma) => [idioma, { translation: TRADUCOES[idioma] }]));

/**
 * Inicia o i18next com as traduções embutidas, sem carregamento pela rede (spec, seção 8): depois desta chamada o
 * t() já devolve o texto no idioma pedido.
 */
export function iniciarI18n(idioma: Idioma): void {
  void i18next.use(initReactI18next).init({
    lng: idioma,
    fallbackLng: IDIOMA_PADRAO,
    supportedLngs: IDIOMAS,
    // Só o idioma exato: pt-BR não procura um pt que não existe
    load: 'currentOnly',
    resources: recursos,
    initAsync: false,
    keySeparator: false,
    nsSeparator: false,
    // O React já escapa o que renderiza
    interpolation: { escapeValue: false },
  });
}
