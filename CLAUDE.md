# BrewApp — contexto para agentes

Diário de adega para cervejarias: acompanhamento estruturado do lote do mosto resfriado ao pré-envase, com registro offline no mobile e comparação de lotes. Projeto pessoal e de portfólio (repositório público). MVP para uso pessoal; modelo de dados preparado para multi-organização (possível SaaS futuro).

## Fontes da verdade (ler antes de propor qualquer coisa)

| Documento | Conteúdo |
|---|---|
| `docs/specs/2026-10-03-mvp-diario-adega-design.md` | Spec do MVP: domínio, arquitetura, dados, offline, segurança, testes, lista de ADRs (seção 11) |
| `docs/api/design-api.md` | Design da API (contrato a seguir): convenções (seção 1), endpoints por módulo (2–7) |
| `docs/processo/fluxo-de-branches.md` | Branches, commits, merges, rulesets, definição de pronto |
| `docs/sprints/sprint-NN.md` | Plano e resultado de cada sprint |

Divergência entre código e docs: parar e perguntar. Mudança de decisão: atualizar o doc correspondente (e o Jira) junto.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 25, Spring Boot 4, Spring Modulith, Spring Security (Resource Server OIDC), Maven |
| Dados | PostgreSQL 18, Flyway, jOOQ (schema-first, codegen via Testcontainers no build; código gerado fora do Git) |
| Auth | Keycloak (OIDC, Authorization Code + PKCE) |
| Logs | SLF4J + Log4j2 |
| Web/desktop | React + TypeScript (strict) + Vite + Tauri |
| Mobile | React Native (Expo), SQLite local |
| Monorepo | `backend/`, `apps/web/`, `apps/mobile/`, `packages/shared/` (tipos OpenAPI, i18n, catálogo de unidades), `infra/`, `docs/`; pnpm workspaces |
| IDE do usuário | IntelliJ IDEA Community (sem suporte específico de Spring) |

## Regras de arquitetura (não negociáveis sem ADR)

- Monolito modular. Módulos: `organizacao`, `recipiente`, `lote`, `adega`, `comparacao`, núcleo `shared`. Um schema Postgres por módulo; nenhum módulo lê tabelas de outro.
- Direção única de dependência: Adega → Lote, Adega → Recipiente; Comparação → Lote/Adega. Ciclo quebra o build (`ApplicationModules.verify()`).
- Chamadas síncronas na mesma transação quando preciso (Adega chama Lote: aceita eventos?, registrar inoculação, corrigir instante zero, atualizar situação). Inversão de dependência para ocupação: porta `ConsultaOcupacaoRecipientes` declarada em Recipiente, implementada em Adega.
- Eventos de domínio (outbox Modulith) só para consumidores que apenas escutam (futuro serviço de Alertas via RabbitMQ).
- Hexagonal por módulo: `domain/` (Java puro, sem Spring/jOOQ), `application/` (casos de uso + portas), `adapter/in/web`, `adapter/out/persistence`. Records do jOOQ não saem da persistência (ArchUnit).
- Diário de eventos ≠ Event Sourcing. Eventos são imutáveis (trigger bloqueia UPDATE/DELETE); correção por retificação.
- Materializações mantidas pela aplicação na mesma transação; triggers só para invariantes técnicas (imutabilidade, `atualizado_em`, auditoria).
- Tempo relativo à inoculação calculado na consulta, nunca materializado.
- Exclusão: eventos nunca; cadastros por inativação lógica (`inativado_em`/`inativado_por`, índices únicos parciais); lote por estado; dados técnicos por limpeza física.
- Multi-tenant: `organization_id` em toda tabela de negócio, FKs compostas, RLS com `SET LOCAL app.organization_id` por transação, app conecta com role não-dono.
- `java.time.Clock` injetado; nunca `Instant.now()` direto.
- Testes de persistência só com Postgres real (Testcontainers); nunca H2. Fakes antes de mocks para repositórios.

## Convenções de API (resumo; detalhes em `docs/api/design-api.md` seção 1)

- Base `/api/v1`, recursos em português, plural, kebab-case.
- Organização só pelo header `X-Organization-Id`; recurso de outra organização responde 404.
- Toda listagem paginada por keyset (`limite`, `cursor` opaco, `{ itens, proximoCursor }`, sem total). Exceções registradas: `GET /me` e `GET /comparacoes`.
- Erros: Problem Details (RFC 9457) com `code` estável; o backend não traduz mensagens.
- Lock otimista: `ETag` + `If-Match` (412/428). Sem `DELETE` de negócio; inativação e mudança de estado são ações `POST`.
- IDs UUIDv7; eventos com id gerado pelo cliente (idempotência).
- Unidades: catálogo único (seção 1.11); API devolve valor informado e canônico.
- Toda rota exige autenticação e declara uma permissão (deny by default).

## Jira

| Item | Valor |
|---|---|
| Site / cloudId | `jorgealvmombach.atlassian.net` / `71623116-f0c1-4f7d-a387-180a4902dbf6` |
| Projeto | `BREW` (Scrum, team-managed) |
| Story points | `customfield_10016` |
| Sprint | `customfield_10020` (Brew-01 = id 3, 05/10–19/10/2026) |

| Epic | Chave |
|---|---|
| Fundação técnica | BREW-5 |
| Identidade e organização | BREW-6 |
| Recipientes | BREW-7 |
| Lotes | BREW-8 |
| Diário de adega | BREW-9 |
| Mobile e offline | BREW-10 |
| Comparação de lotes | BREW-11 |

Histórias: BREW-12 a BREW-67 (56 histórias; 229 pontos estimados, BREW-67 ainda sem estimativa). Criar ou alterar issues somente com aprovação explícita do usuário.

## Git

- Branches: `main` ← `epic/BREW-<n>` ← `feat/BREW-<n>` (histórias/tasks) ou `hotfix/BREW-<n>` (bugs). Chave exata do Jira (`BREW-5`, nunca `BREW-05`).
- Uma epic e uma branch de trabalho por vez. Trabalho entra na epic por PR; sincronização `main` → epic por merge direto.
- Somente merge commit (squash/rebase desabilitados) para não duplicar `#time` no Jira. Não reescrever commits com `#time` já enviados.
- Commit: `BREW-<n> #time <tempo> [Descrição em pt-BR]`. Mensagem começa direto pela chave, sem prefixo de tipo (`feat/BREW-12` na mensagem impede o Smart Commit de registrar o `#time`). O prefixo `feat/`/`hotfix/` fica só no nome da branch.
- História pronta = mergeada na epic com CI verde.
- **O agente nunca faz commit, push nem PR.** Deixa as mudanças no working tree e resume o que mudou.
- Repositório público: nada sensível em commits; `.env` fora do Git; gitleaks no pre-commit.

## Sprint atual (Brew-01)

Objetivo: backend Java 25/Spring Boot 4 subindo localmente com Postgres e Keycloak via docker-compose, com regras de arquitetura verificadas no build.

Ordem: BREW-12 (2) → BREW-13 (3) → BREW-14 (3) → BREW-21 (1) [compromisso] → BREW-15 (5) [P1] → BREW-16 (3) [P2].

## Como trabalhar neste projeto

- Para cada história: ler os critérios de aceite no Jira, propor um plano técnico em fases numeradas e parar entre cada fase para validação.
- Ao concluir uma história: checar os critérios de aceite um a um e a Definition of Done (BREW-21), e lembrar o usuário de registrar as horas.
- Decisão de arquitetura nova ou alterada: propor ADR em `docs/adr/`.
- Na retro: preencher a seção "Resultado" do `docs/sprints/sprint-NN.md` com pontos concluídos e horas.
