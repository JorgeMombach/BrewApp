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

### Ampliação em 10/10 (backlog original concluído)

O backlog original (17 pontos) foi concluído em 07/10. Em vez de encerrar o sprint antes do prazo, o restante da epic BREW-5 foi puxado para ele, para que a velocidade medida em 19/10 corresponda a um sprint completo. Ordem de execução:

| Ordem | Prioridade | Item | Pontos | Motivo da posição |
|---|---|---|---|---|
| 1 | Compromisso | BREW-68 README da raiz (task) | 1 | Repositório público sem README |
| 2 | Compromisso | BREW-69 Mensagens de exceção sem mascaramento nos logs (bug) | 2 | Precisa estar resolvido antes da primeira tabela com dado pessoal (BREW-24) |
| 3 | Compromisso | BREW-20 Pipeline de CI | 5 | Encerra a exceção da DoD e leva a checagem do jOOQ para a CI |
| 4 | Compromisso | BREW-18 Pacote compartilhado e contrato da API | 3 | Base das histórias 45, 17 e 19 |
| 5 | Compromisso | BREW-45 Catálogo de unidades | 5 | Usa os casos de teste compartilhados da 18; pré-requisito de Recipientes e Lotes |
| 6 | Se der | BREW-17 App web e desktop | 3 | Usa os tipos gerados na 18; maior risco do sprint (toolchain do Rust para o Tauri) |
| 7 | Se der | BREW-19 Internacionalização | 3 | Depende da 17 e da 18 |
| 8 | Se der | BREW-22 ADRs restantes | 2 | Fecha a epic; o ADR 0013 saiu na própria BREW-18 (ver decisões de 13/10) |

- **Novo compromisso:** 17 pontos já entregues + 16 pontos (itens 1 a 5) = 33 pontos
- **Se der:** + 8 pontos (itens 6 a 8) = 41 pontos, com a epic BREW-5 fechada

Concluída a epic, ela é mergeada na `main` (primeira entrega na `main`), com os status checks obrigatórios já ativos no ruleset `protecao-main`.

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
| 07/10 | Código gerado pelo jOOQ passa a ser versionado (BREW-15), revertendo a spec 5.1: o projeto compila logo após o clone e a pull request mostra o impacto das migrations. O codegen sai do build padrão para o profile `jooq-codegen`, e uma checagem de divergência (regenerar e falhar se houver diferença) entra na DoD e no template de pull request. Até a BREW-20 a checagem é local; depois, vira job da CI. |
| 10/10 | Review antecipada do backlog original: 6 de 6 histórias atendem aos critérios de aceite; build com 95 testes passando. Encontrado um bug de segurança nos logs (BREW-69) e criada a task do README (BREW-68). |
| 10/10 | Sprint ampliado com o restante da epic BREW-5, em vez de encerrado antes do prazo (ver "Ampliação em 10/10"). |
| 10/10 | BREW-22 reduzida de 3 para 2 pontos: os ADRs 0002 e 0014 foram escritos durante as histórias BREW-15 e BREW-16. |
| 10/10 | Jobs de CI de front saem da BREW-20 e entram nas histórias que criam cada parte (BREW-17 app web, BREW-18 contrato da API, BREW-19 traduções). A BREW-20 entrega backend, segurança e a checagem do jOOQ, sem ficar bloqueada pelo front. |
| 12/10 | CI (BREW-20) num único workflow com um job por área do monorepo, acionado por filtro de caminho, e um job final `CI concluída` como único check obrigatório do ruleset. Workflows separados por área deixariam o check obrigatório pendente quando a área não muda. A spec (seção 10) foi ajustada. CodeQL em workflow próprio, bloqueando pela regra de code scanning do ruleset. |
| 12/10 | Formatação com Spotless e Palantir Java Format (4 espaços, igual ao `.editorconfig`), só nos arquivos alterados. O build local formata sozinho; a CI só verifica, na pull request. A CI não corrige nem commita por conta própria. |
| 12/10 | Trivy só nas dependências (`pom.xml` e `pnpm-lock.yaml`), quebrando em vulnerabilidade crítica que já tem correção; as sem correção ficam no relatório da aba Security. Varredura de imagem (spec 9.6) entra quando existir a imagem do backend, junto com o CD. |
| 12/10 | Pull requests do Dependabot vão direto para a `main`, como hotfix de manutenção, seguidas da sincronização na epic. Imagens do docker-compose ficam fora do Dependabot enquanto a imagem do PostgreSQL estiver repetida em três lugares (ADR 0002). |
| 12/10 | Fim da exceção de 03/10 à Definition of Done: a partir do merge da BREW-20 na epic, história pronta exige CI verde. A checagem de divergência do jOOQ passa de passo local a job da CI. |
| 13/10 | ADR 0013 (contrato da API gerado a partir do código) escrito na BREW-18, junto com a implementação, e não na BREW-22: a Definition of Done pede o ADR na entrega que introduz a decisão. A BREW-22 fica com o template, os ADRs 0001 e 0003 e o índice. |
| 13/10 | Contrato OpenAPI exportado por teste de integração para `packages/shared/openapi/openapi.json` e tipos TypeScript gerados a partir dele, ambos versionados, com checagem de divergência na CI (jobs do backend e do contrato). Documento e Swagger UI só no perfil `dev`. Detalhes no ADR 0013. |
| 13/10 | TypeScript 5.9 no monorepo: o `openapi-typescript` não suporta o TypeScript 7 (compilador em Go). Vale também para o app web (BREW-17). |
| 13/10 | Casos compartilhados de densidade seguem a tabela de extrato da ASBC, com tolerância por caso (0,0001 SG e 0,01 °P), porque a conversão entre Plato e SG não é linear e as fórmulas são aproximações. |
| 13/10 | Ajuste da CI encontrado depois da BREW-20, feito na BREW-18: `cancel-in-progress` só em pull request, no `ci.yml` e no `codeql.yml`. No push para `main` e `epic/**`, cancelar deixava um commit sem resultado de CI ou de CodeQL. JavaScript/TypeScript entra na matriz do CodeQL já nesta história, com o primeiro código TypeScript. |
| 13/10 | Prettier e ESLint entram na BREW-17, configurados na raiz do monorepo e valendo também para o `packages/shared`, que entrou na BREW-18 sem formatador. O job do contrato passa a rodar o lint do pacote. Critério incluído na BREW-17 no Jira. |

## Resultado (preencher na retro)

| História | Pontos | Concluída? | Horas gastas | Observações |
|---|---|---|---|---|
| BREW-12 | 2 | Sim (03/10) | 5h | 2h não registradas no Jira: commit com prefixo `feat/` na mensagem |
| BREW-13 | 3 | Sim (03/10) | 2h | |
| BREW-14 | 3 | Sim (04/10) | 2h | |
| BREW-21 | 1 | Sim (07/10) | 0,5h | |
| BREW-15 | 5 | Sim (07/10) | 1h | |
| BREW-16 | 3 | Sim (07/10) | 1h | Bug BREW-69 encontrado na review |
| BREW-68 | 1 | | | Incluída em 10/10 |
| BREW-69 | 2 | | | Incluído em 10/10 |
| BREW-20 | 5 | | | Incluída em 10/10 |
| BREW-18 | 3 | | | Incluída em 10/10 |
| BREW-45 | 5 | | | Incluída em 10/10 |
| BREW-17 | 3 | | | Incluída em 10/10 |
| BREW-19 | 3 | | | Incluída em 10/10 |
| BREW-22 | 2 | | | Incluída em 10/10 |

**Parcial em 10/10 (backlog original):** 17 pontos em 11,5h, cerca de 0,7h por ponto. Infraestrutura com IA pareando saiu barata; o custo ficou em decisões e documentação. Pontos e horas ainda não andam juntos (a BREW-15, de 5 pontos, levou 1h; a BREW-12, de 2 pontos, levou 5h). Histórias de negócio devem custar mais, com o teste manual de funcionalidades entrando nas horas.

- **Pontos concluídos (velocidade do sprint):**
- **Horas totais:**
- **Horas por ponto (referência, não meta):**

### Retro

- **O que funcionou:**
- **O que atrapalhou:**
- **O que muda no próximo sprint:**
