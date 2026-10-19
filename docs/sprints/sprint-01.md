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
| 13/10 | Catálogo de unidades (BREW-45): `BRIX` é a escala de sacarose, com a mesma conversão do Plato e casos próprios tirados da tabela do USDA. A correção da leitura de refratômetro com álcool depende do OG do lote e virou a história BREW-70, na epic do Diário de adega. |
| 13/10 | `Quantidade` guarda o valor informado como `BigDecimal`; as fórmulas rodam em `double`, e o valor canônico convertido sai com 6 casas, para o ruído do ponto flutuante não chegar à API nem ao banco. Unidade de outra grandeza responde 422 `UNIDADE_INVALIDA`, código do catálogo descrito na seção 1.11 do design da API. |
| 13/10 | O pacote `shared.unidade` é interface nomeada do Spring Modulith (`@NamedInterface`, com a dependência `spring-modulith-api`), porque o domínio dos módulos de negócio usa `Quantidade`. Os demais subpacotes do shared continuam internos; a decisão geral (interfaces nomeadas ou shared aberto) fica para a primeira história de negócio. A regra de Java puro do ArchUnit passa a cobrir o `shared.unidade`. |
| 13/10 | Prettier e ESLint entram na BREW-17, configurados na raiz do monorepo e valendo também para o `packages/shared`, que entrou na BREW-18 sem formatador. O job do contrato passa a rodar o lint do pacote. Critério incluído na BREW-17 no Jira. |
| 16/10 | Lint do TypeScript (BREW-17) com o `strictTypeChecked` do typescript-eslint, proibindo `any`, casts e `!` (o `as const` continua permitido). Prettier com 120 colunas e aspas simples, só sobre o TypeScript e as configs de `apps/` e `packages/`: Java tem o Spotless, e documentação, infraestrutura, workflows, arquivos gerados e casos de teste compartilhados ficam de fora. |
| 16/10 | ESLint fixado na 10.12.0: o pnpm 12 recusou a 10.13.0, publicada no mesmo dia, pela idade mínima de release, e incluiu sozinho uma exceção no `pnpm-workspace.yaml`. A exceção foi desfeita: versão recém-publicada espera, a política não abre exceção. |
| 16/10 | App web com React Router 8 no modo data (`createBrowserRouter`), com a árvore de rotas separada do roteador para os testes usarem a mesma. Os textos da interface ficam num arquivo provisório só em português até a BREW-19, que o troca pelas chaves de tradução; enquanto isso, nenhum componente escreve texto fixo fora dele. Biblioteca de componentes fica para a primeira tela de negócio. |
| 16/10 | Cliente HTTP com o `openapi-fetch` e toda falha entregue às telas como `ErroDaApi`, com os códigos do cliente `SEM_CONEXAO` e `RESPOSTA_INESPERADA` na seção 1.6 do design da API (ADR 0017). A instância do cliente, com a URL do backend vinda do `.env`, entra com a primeira tela que chamar um endpoint (provavelmente o `GET /me`, na BREW-6). |
| 16/10 | Casca Tauri 2 em `apps/web/src-tauri`, abrindo o app em `127.0.0.1:1420`, a origem do client desktop do Keycloak. A compilação do Rust fica fora da CI (cerca de 5 minutos na primeira vez) e é conferida localmente; o job `web` da CI cobre o build do app que o Tauri empacota. O CORS do `.env.example` passa a aceitar as origens do desktop, e o Dependabot ganha a entrada `cargo`. |
| 16/10 | A CSP do desktop aceita a API e o Keycloak só em `localhost`, o que serve ao uso local do MVP; ganha os endereços reais junto com o primeiro deploy. O login no app empacotado (sem o servidor do Vite) fica para a BREW-6. |
| 19/10 | Internacionalização (BREW-19) com i18next e traduções em `packages/shared/i18n`, com chaves planas tipadas a partir do pt-BR e a convenção `app.`, `dominio.`, `erro.` e `validacao.` (ADR 0012). O ADR leva o número 12 por ser o item 12 da lista da spec (seção 11), e não o próximo número livre. |
| 19/10 | Traduções no banco, quando vierem, começam com o banco como fonte e os arquivos exportados para o build, como já se faz em outros projetos; a publicação em CDN fica para um segundo passo. A spec (seção 8) foi ajustada. |
| 19/10 | Número digitado aceita vírgula ou ponto como separador decimal em qualquer idioma, sem separador de milhar: o `Intl` do `es` usa vírgula, mas no México se usa ponto. Em volume e massa, `1.000` vira 1; a primeira tela com esses campos (BREW-7) precisa tratar isso. |
| 19/10 | Idioma em uso: escolha manual guardada no navegador, depois as preferências do navegador e, por fim, `pt-BR`. A preferência `idioma` do `GET /me` entra na frente na BREW-6, quando o seletor também passa a gravá-la. |
| 19/10 | Garantias das traduções: chave inexistente quebra a compilação, o ESLint barra texto fixo no JSX e nos atributos lidos pelo usuário, o job `traducoes` da CI compara os três idiomas, e um teste do backend falha se algum código de erro ficar sem tradução. As traduções entraram no filtro do job do backend. |
| 19/10 | ADRs da fundação (BREW-22): template e índice em `docs/adr/`, ADR 0001 (monolito modular) e ADR 0003 (hexagonal por módulo). O ADR 0017, que pegou o próximo número livre, entrou como item 17 na lista da spec (seção 11): decisão nova vai para o fim da lista. Os ADRs previstos ficam no índice com a epic onde devem aparecer. |
| 19/10 | A regra do ArchUnit para a direção entre camadas (aplicação sem depender de adaptador, domínio sem depender de aplicação) entra na primeira história de negócio: sem módulo de negócio, a regra não teria classes para analisar e falharia. Até lá, só o domínio em Java puro e o jOOQ restrito à persistência são verificados (ADR 0003). |
| 19/10 | Definition of Done e template de pull request: o critério de decisões de arquitetura passa a pedir também a atualização do índice de ADRs (`docs/adr/README.md`), inclusive o status de um ADR previsto que é escrito ou de um ADR substituído. |
| 19/10 | Review final: 14 de 14 itens atendem aos critérios de aceite; `mvnw verify` com 216 testes e front (lint, typecheck, 164 testes, build) passando; CI verde em todos os merges na epic desde a BREW-20. Achados: as 8 pull requests da ampliação (#7 a #14) não usaram o template, então a checagem de regressão e o checklist da DoD não ficaram registrados nelas (tratado na retro, sem editar as PRs depois do merge e sem criar task de checagem automática por enquanto); BREW-22 movida de In Progress para Production. Pontos de atenção para as próximas epics anotados na memória do projeto. |
| 19/10 | Versionamento e releases, ponto que faltava mapear antes do primeiro merge na `main`: cada epic na `main` é uma release, com uma versão única para todo o sistema em SemVer 0.x (epic sobe o *minor*, hotfix em produção sobe o *patch*, Dependabot não gera versão), tag `vX.Y.Z` no merge commit como fonte da verdade, `CHANGELOG.md` na raiz no formato Keep a Changelog e GitHub Release com o mesmo texto. Feito direto na epic, sem task no Jira. A BREW-5 sai como 0.1.0. Detalhes em `docs/processo/fluxo-de-branches.md`. |

## Resultado (preencher na retro)

| História | Pontos | Concluída? | Horas gastas | Observações |
|---|---|---|---|---|
| BREW-12 | 2 | Sim (03/10) | 5h | 2h não registradas no Jira: commit com prefixo `feat/` na mensagem |
| BREW-13 | 3 | Sim (03/10) | 2h | |
| BREW-14 | 3 | Sim (04/10) | 2h | |
| BREW-21 | 1 | Sim (07/10) | 0,5h | |
| BREW-15 | 5 | Sim (07/10) | 1h | |
| BREW-16 | 3 | Sim (07/10) | 1h | Bug BREW-69 encontrado na review |
| BREW-68 | 1 | Sim (12/10) | 1,5h | Incluída em 10/10. Só 0,5h foi o README: 1h é da review e ampliação de 10/10 e da skill de cerimônias, lançada nela |
| BREW-69 | 2 | Sim (12/10) | 1h | Incluído em 10/10 |
| BREW-20 | 5 | Sim (12/10) | 2h | Incluída em 10/10. A partir dela, CI verde em todos os merges na epic |
| BREW-18 | 3 | Sim (13/10) | 2h | Incluída em 10/10 |
| BREW-45 | 5 | Sim (13/10) | 2h | Incluída em 10/10. Gerou a BREW-70 (refratômetro), ainda sem estimativa |
| BREW-17 | 3 | Sim (16/10) | 3h | Incluída em 10/10. Uma rodada de CI vermelha na PR antes do merge |
| BREW-19 | 3 | Sim (19/10) | 2h | Incluída em 10/10 |
| BREW-22 | 2 | Sim (19/10) | 0,5h | Incluída em 10/10. Ficou In Progress no Jira depois do merge; movida na review |

**Parcial em 10/10 (backlog original):** 17 pontos em 11,5h, cerca de 0,7h por ponto. Infraestrutura com IA pareando saiu barata; o custo ficou em decisões e documentação. Pontos e horas ainda não andam juntos (a BREW-15, de 5 pontos, levou 1h; a BREW-12, de 2 pontos, levou 5h). Histórias de negócio devem custar mais, com o teste manual de funcionalidades entrando nas horas.

- **Pontos concluídos (velocidade do sprint):** 41 de 41 (14 de 14 itens); compromisso original de 9 pontos e compromisso ampliado de 33
- **Horas totais:** 25,5h (23,5h no Jira + 2h da BREW-12 sem registro), contra cerca de 16h de capacidade estimada. Sem a 1h de cerimônias lançada na BREW-68, 24,5h de entrega
- **Horas por ponto (referência, não meta):** 0,6h (24,5h de entrega / 41 pontos). Backlog original: 0,68h (11,5h em 17 pontos); ampliação: 0,54h (13h em 24 pontos)
- **Comparação com as sprints anteriores:** primeira sprint, sem base de comparação. A velocidade de referência para a Brew-02 é 41 pontos, com ressalvas (ver padrões)
- **Padrões nos números:**
  - Front custou mais por ponto: BREW-17 e BREW-19 somaram 5h em 6 pontos (0,83h/ponto), contra 6h em 13 pontos (0,46h/ponto) para BREW-20, BREW-18 e BREW-45.
  - Pontos e horas continuam descolados nos dois sentidos: BREW-15 (5 pontos, 1h) e BREW-17 (3 pontos, 3h).
  - As horas passaram 60% da capacidade estimada. A velocidade de 41 pontos veio de infraestrutura pareada com IA, sem teste manual de funcionalidade; histórias de negócio devem render menos pontos por hora.

### Retro

- **O que funcionou:** o volume de atividades foi condizente com duas semanas, e o ritmo da sprint funcionou como estava.
- **O que atrapalhou:** nada que o Jorge tenha sentido como obstáculo. Da review: as 8 pull requests da ampliação saíram sem o template, então a checagem de regressão e o checklist da DoD ficaram só nas sessões de trabalho, sem registro nas PRs.
- **O que muda no próximo sprint:** manter o mesmo formato. Para o planning: as 25,5h efetivas, e não as 16h estimadas, são a capacidade real de referência; e a descrição das pull requests volta a seguir o template.
