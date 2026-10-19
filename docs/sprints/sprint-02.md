# Sprint 2 (Brew-02)

- **Período:** 20/10/2026 a 02/11/2026
- **Feriados no período:** 02/11 (Finados), último dia do sprint
- **Capacidade estimada:** cerca de 25 horas, a mesma da Brew-01 (25,5h efetivas)
- **Velocidade de referência:** 41 pontos (média de 1 sprint, a Brew-01). O compromisso fica bem abaixo dela: a Brew-01 foi só de infraestrutura pareada com IA e sem teste manual, e esta é a primeira sprint de histórias de negócio, com telas, teste manual e decisões novas.

## Objetivo

Usuário entra pela web e pelo desktop, é reconhecido pelo backend, e todo acesso a dados fica autorizado por permissão e isolado por organização no banco.

## Backlog da sprint

| Ordem | Prioridade | Item | Pontos | Motivo da posição |
|---|---|---|---|---|
| 1 | Compromisso | BREW-23 Login e logout na web e no desktop | 5 | Pré-requisito de toda tela. A validação do token no backend já existe desde a BREW-14; o trabalho é o front: PKCE na web com token em memória, armazenamento seguro no Tauri, refresh com rotação e logout |
| 2 | Compromisso | BREW-24 Provisionamento do usuário e `GET /me` | 5 | Primeira história de negócio. Reestimada de 3 para 5 pontos por levar as decisões pendentes da fundação |
| 3 | Compromisso | BREW-26 Isolamento entre organizações no banco | 8 | A Definition of Done exige RLS em toda tabela de negócio nova, então o padrão vem antes da tabela de organização e vínculo |
| 4 | Compromisso | BREW-25 Contexto de organização e autorização por permissão | 5 | Usa o vínculo criado na BREW-26 e implanta a regra de que toda rota declara uma permissão |
| 5 | Se der | BREW-27 Criar organização (onboarding) | 3 | Primeiro endpoint de escrita, sobre a base das BREW-25 e 26 |
| 6 | Se der | BREW-28 Consultar e editar organização (lock otimista) | 5 | Implanta o lock otimista genérico, reaproveitado por Recipientes e Lotes |

- **Compromisso:** 23 pontos
- **Se der:** 8 pontos (total de 31)

Ficam para a Brew-03: BREW-29 a 33, e a BREW-67, ainda sem estimativa e dependente da BREW-31 (trilha de auditoria).

### Pontos de atenção que vencem nesta sprint

| História | Ponto |
|---|---|
| BREW-23 | Login no app Tauri empacotado, fora do servidor do Vite. ADR 0010 (autenticação delegada a OIDC) |
| BREW-24 | Decisão sobre o `shared` no Spring Modulith (interfaces nomeadas ou módulo aberto); regra do ArchUnit para a direção entre camadas (ADR 0003); instância do cliente HTTP no app web, com a URL vinda do `.env`; preferência de idioma do `GET /me` na detecção de idioma; ADR 0015 (exceção de paginação do `GET /me`) |
| BREW-26 | ADR 0011 (isolamento entre organizações em três camadas) |

## Riscos

| Risco | Mitigação |
|---|---|
| Armazenamento seguro do token no Tauri (plugin novo, compilação em Rust) | Começar a BREW-23 pelo desktop; se travar, entregar a web e abrir uma história separada para o desktop |
| RLS com pool de conexões: contexto de organização vazando entre requisições | O teste de vazamento é critério da BREW-26 e é escrito primeiro (TDD) |
| Primeira história de negócio com várias decisões de base ao mesmo tempo | BREW-24 reestimada; a decisão sobre o `shared` é a primeira fase da história |
| Feriado no último dia | Review e retro em 02/11 ou 03/11 |

## Cerimônias

| Data | Evento |
|---|---|
| 19/10 | Planning |
| ~27/10 | Checagem de meio de sprint: o compromisso ainda cabe? |
| 02/11 ou 03/11 | Review e retro |

## Decisões durante o sprint

| Data | Decisão |
|---|---|
| 19/10 | BREW-24 reestimada de 3 para 5 pontos no planning: como primeira história de negócio, leva a estrutura hexagonal do primeiro módulo, a regra de camadas do ArchUnit, a decisão sobre o `shared` no Modulith, a instância do cliente HTTP e o ADR 0015. Total estimado do backlog passa de 231 para 233 pontos. |
| 19/10 | Alerta do `glib` 0.18.5 (crate Rust, dependência do Tauri 2 pelo gtk-rs 0.18) dispensado no Dependabot (*Tolerable risk*) e no code scanning do Trivy (*Won't fix*): a correção só existe no `glib` 0.20, o Tauri ainda prende a versão, e o crate só entra no build Linux do desktop, que não é distribuído. Reavaliar antes de distribuir o desktop para Linux. O alerta do Trivy pode reaparecer se uma atualização mexer na linha do `Cargo.lock`; nesse caso, dispensar de novo com a mesma justificativa. |
| 19/10 | Dependabot: majors ignorados com `@dependabot ignore this major version` no TypeScript 6 (o `openapi-typescript` 7.13 exige `^5.x`, ADR 0013) e no `@types/node` 26 (os tipos acompanham o Node do runtime, que é o 24). Reavaliar quando o `openapi-typescript` aceitar TypeScript 6 ou o Node subir de major. As demais PRs (#16, #17, #18 e #21) foram mergeadas direto na `main` e entram no changelog da 0.2.0. |
| 19/10 | O Dependabot regravou o `mvnw.cmd` com CRLF no repositório, contra o `.gitattributes` (LF no repositório, CRLF no disco). Normalizado com `git add --renormalize` no primeiro commit da `epic/BREW-6`. Pode se repetir a cada atualização do Maven Wrapper. |

## Resultado (preencher na retro)

| Item | Pontos | Concluído? | Horas gastas | Observações |
|---|---|---|---|---|
| BREW-23 | 5 | | | |
| BREW-24 | 5 | | | |
| BREW-26 | 8 | | | |
| BREW-25 | 5 | | | |
| BREW-27 | 3 | | | Se der |
| BREW-28 | 5 | | | Se der |

- **Pontos concluídos (velocidade da sprint):**
- **Horas totais:**
- **Horas por ponto (referência, não meta):**
- **Comparação com as sprints anteriores:**

### Retro

- **O que funcionou:**
- **O que atrapalhou:**
- **O que muda no próximo sprint:**
