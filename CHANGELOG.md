# Changelog

Todas as mudanças relevantes do BrewApp ficam registradas aqui, uma seção por versão.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), e as versões seguem o [Versionamento Semântico](https://semver.org/lang/pt-BR/). Enquanto o MVP estiver em construção, o projeto fica na série 0.x: cada epic que chega à `main` gera uma versão nova, e cada correção feita direto na `main` gera um patch.

## [0.1.0] - 2026-10-19

Primeira versão: a fundação técnica (epic BREW-5). Ainda não tem funcionalidade para o usuário final. Ela entrega a base sobre a qual as próximas epics vão construir as telas e as regras do diário de adega.

### Adicionado

- Monorepo com o backend, o app web e desktop, o pacote compartilhado, a infraestrutura local e a documentação, com as convenções de commit e de branches e o README do projeto (BREW-12, BREW-68).
- Ambiente local com PostgreSQL 18 e Keycloak via docker-compose, acessível apenas pela própria máquina (BREW-13).
- Backend em Java 25 e Spring Boot 4, organizado como monolito modular com Spring Modulith. Toda rota exige autenticação por padrão, e os erros saem no formato Problem Details com código estável (BREW-14).
- Migrations com Flyway e acesso ao banco com jOOQ. O código gerado é versionado e conferido contra as migrations a cada mudança (BREW-15).
- Logs com Log4j2, identificador de correlação por requisição, JSON estruturado em produção e mascaramento de tokens, segredos e e-mails, inclusive nas mensagens e no stack trace das exceções (BREW-16, BREW-69).
- Integração contínua no GitHub Actions: build e testes do backend, regras de arquitetura, formatação, divergência do jOOQ, varredura de segredos, varredura de dependências vulneráveis e CodeQL, além de atualizações automáticas pelo Dependabot (BREW-20).
- Contrato da API gerado a partir do código e tipos TypeScript gerados a partir dele, com casos de teste compartilhados entre Java e TypeScript (BREW-18).
- Catálogo de unidades de medida (densidade, temperatura, pressão, volume e massa), com as mesmas conversões e os mesmos casos de teste no backend e nos clientes (BREW-45).
- Esqueleto do app web em React e TypeScript, empacotado também como aplicação desktop com Tauri, com cliente HTTP tipado pelo contrato da API (BREW-17).
- Interface em português do Brasil, espanhol e inglês, com troca de idioma, números e datas no formato de cada idioma e erros da API traduzidos (BREW-19).
- Definition of Done do projeto e registros de decisão de arquitetura (ADRs) com template e índice (BREW-21, BREW-22).

[0.1.0]: https://github.com/JorgeMombach/BrewApp/releases/tag/v0.1.0
