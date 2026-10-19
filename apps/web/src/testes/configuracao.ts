// Executado antes de cada arquivo de teste (vite.config.ts, test.setupFiles)
import '@testing-library/jest-dom/vitest';
import { IDIOMA_PADRAO } from '@brewapp/shared';
import { cleanup } from '@testing-library/react';
import i18next from 'i18next';
import { afterEach } from 'vitest';
import { iniciarI18n } from '../i18n/configuracaoI18n';

// Os testes procuram os textos em português, qualquer que seja o idioma do navegador do jsdom
iniciarI18n(IDIOMA_PADRAO);

// Sem os globais do Vitest, a Testing Library não desmonta sozinha o que cada teste renderizou
afterEach(async () => {
  cleanup();
  await i18next.changeLanguage(IDIOMA_PADRAO);
});
