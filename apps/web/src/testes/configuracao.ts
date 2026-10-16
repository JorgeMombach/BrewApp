// Executado antes de cada arquivo de teste (vite.config.ts, test.setupFiles)
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Sem os globais do Vitest, a Testing Library não desmonta sozinha o que cada teste renderizou
afterEach(() => {
  cleanup();
});
