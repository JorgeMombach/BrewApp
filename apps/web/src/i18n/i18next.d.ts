import type { Traducoes } from '@brewapp/shared';

// Chaves do t() tipadas pelas traduções do packages/shared: chave inexistente quebra a compilação
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation';
    resources: { translation: Traducoes };
    // Chaves planas: o ponto faz parte do nome (app.paginaInicial.titulo), não é aninhamento
    keySeparator: false;
    nsSeparator: false;
    // Nem com defaultValue uma chave inexistente passa
    strictKeyChecks: true;
  }
}
