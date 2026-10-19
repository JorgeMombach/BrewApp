// @ts-check
// Lint de todo o TypeScript do monorepo (apps/ e packages/). Formatação é do Prettier, não do ESLint.
import js from '@eslint/js';
import reactHooks from 'eslint-plugin-react-hooks';
import { defineConfig, globalIgnores } from 'eslint/config';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default defineConfig(
  globalIgnores([
    '**/node_modules/',
    '**/dist/',
    '**/coverage/',
    '**/src-tauri/target/',
    'backend/',
    'infra/',
    // Gerados pelo openapi-typescript (ADR 0013): corrigir no gerador, não no arquivo
    '**/*.gerado.ts',
  ]),
  js.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    extends: [tseslint.configs.strictTypeChecked, tseslint.configs.stylisticTypeChecked],
    languageOptions: {
      parserOptions: {
        // Cada arquivo é checado com o tsconfig do pacote a que pertence
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      // Sem cast para silenciar o compilador: tipo errado se corrige na definição (o "as const" continua permitido).
      // O no-explicit-any e o no-non-null-assertion já vêm do strictTypeChecked
      '@typescript-eslint/consistent-type-assertions': ['error', { assertionStyle: 'never' }],
    },
  },
  {
    // App web e desktop: código de navegador com React
    files: ['apps/web/src/**/*.{ts,tsx}'],
    extends: [reactHooks.configs.flat['recommended-latest']],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    // Nenhum texto fixo na interface (Definition of Done): todo texto visível ou lido pelo leitor de tela vem do t().
    // Os testes ficam de fora, porque procuram os textos já traduzidos
    files: ['apps/web/src/**/*.tsx'],
    ignores: ['**/*.test.tsx'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: 'JSXText[value=/\\S/]',
          message: 'Texto fixo na interface: use uma chave de tradução com o t().',
        },
        {
          selector: ':matches(JSXElement, JSXFragment) > JSXExpressionContainer > :matches(Literal, TemplateLiteral)',
          message: 'Texto fixo na interface: use uma chave de tradução com o t().',
        },
        {
          // Literal direto (aria-label="...") ou entre chaves (aria-label={'...'}); o argumento do t() não conta
          selector: [
            'JSXAttribute[name.name=/^(aria-label|aria-description|title|alt|placeholder)$/] > Literal',
            'JSXAttribute[name.name=/^(aria-label|aria-description|title|alt|placeholder)$/] > JSXExpressionContainer > :matches(Literal, TemplateLiteral)',
          ].join(', '),
          message:
            'Texto fixo em atributo lido pelo usuário ou pelo leitor de tela: use uma chave de tradução com o t().',
        },
      ],
    },
  },
);
