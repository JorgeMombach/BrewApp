# Sprint 1 (Brew-01)

- **Período:** 05/10/2026 a 19/10/2026
- **Feriado no período:** 12/10 (Nossa Senhora Aparecida)
- **Capacidade estimada:** 6 a 10 horas por semana, cerca de 16 horas no sprint
- **Natureza do sprint:** calibração. O objetivo secundário é medir a velocidade real, não maximizar entregas.

## Objetivo

Backend em Java 25/Spring Boot 4 subindo localmente com Postgres e Keycloak via docker-compose, com as regras de arquitetura verificadas no build.

## Backlog do sprint

| Prioridade | História | Pontos |
|---|---|---|
| P0 | BREW-12 Estrutura do monorepo e convenções | 2 |
| P0 | BREW-13 Ambiente local com docker-compose | 3 |
| P0 | BREW-14 Esqueleto do backend | 3 |
| P0 | BREW-21 Definition of Done | 1 |
| P1 | BREW-15 Pipeline Flyway + codegen jOOQ | 5 |
| P2 | BREW-16 Logs e correlação de requisições | 3 |

- **Compromisso (P0):** 9 pontos
- **Se der (P1):** 14 pontos
- **Extra (P2):** 17 pontos

O compromisso fica de propósito abaixo do palpite inicial de velocidade (10 a 12 pontos por sprint). Se a BREW-15 passar para o Sprint 2, isso também é informação útil para a calibração.

## Premissas de estimativa

- Story points em Fibonacci, por tamanho relativo.
- Referência: BREW-12 (monorepo) = 2 pontos.
- A velocidade passa a ser a média dos pontos concluídos nos últimos sprints, planejando com 20 a 30% de folga.

## Riscos

| Risco | Mitigação |
|---|---|
| Curva de aprendizado em Java 25, Spring Boot 4 e Spring Modulith | Esqueleto mínimo primeiro; recursos novos aos poucos |
| Configuração do realm do Keycloak costuma levar mais tempo do que parece | Exportar o realm assim que funcionar e versioná-lo |
| Codegen do jOOQ com Testcontainers no Maven, com Docker no Windows | Validar o Docker Desktop no primeiro dia |

## Cerimônias

| Data | Evento |
|---|---|
| 05/10 | Planning (30 min): confirmar este plano e iniciar o sprint no Jira |
| ~09/10 | Checagem de meio de sprint (10 min): o P1 ainda cabe? |
| 19/10 | Review e retro (30 min) |

## Decisões durante o sprint

| Data | Decisão |
|---|---|
| 03/10 | Exceção à Definition of Done: enquanto a BREW-20 (pipeline de CI no GitHub Actions) não for entregue, uma história é considerada pronta quando mergeada na branch da epic, sem exigir CI verde. A partir da entrega da BREW-20, volta a valer a regra normal. |
| 07/10 | Definition of Done revisada e aprovada (BREW-21), antes da primeira história de negócio. Vale a partir das próximas entregas; o checklist fica no template de pull request. |

## Resultado (preencher na retro)

| História | Pontos | Concluída? | Horas gastas | Observações |
|---|---|---|---|---|
| BREW-12 | 2 | | | |
| BREW-13 | 3 | | | |
| BREW-14 | 3 | | | |
| BREW-21 | 1 | | | |
| BREW-15 | 5 | | | |
| BREW-16 | 3 | | | |

- **Pontos concluídos (velocidade do sprint):**
- **Horas totais:**
- **Horas por ponto (referência, não meta):**

### Retro

- **O que funcionou:**
- **O que atrapalhou:**
- **O que muda no próximo sprint:**
