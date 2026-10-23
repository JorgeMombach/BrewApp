import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  // Um único .env, o da raiz do monorepo: o Vite só expõe ao app as variáveis com prefixo VITE_
  envDir: '../..',
  // Mantém visíveis os logs do Rust no terminal do "tauri dev"
  clearScreen: false,
  server: {
    // Origem cadastrada no client brewapp-web do Keycloak e no CORS do backend: outra porta quebra o login.
    // O desktop sobe na 127.0.0.1:1420 (script dev:desktop), a origem do client brewapp-desktop
    port: 5173,
    strictPort: true,
    watch: {
      // O target/ do Rust muda a cada compilação e não faz parte do app web
      ignored: ['**/src-tauri/**'],
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/testes/configuracao.ts'],
  },
});
