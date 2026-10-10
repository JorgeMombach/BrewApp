---
name: cerimonias-sprint
description: Conduz review, retro e planning de uma sprint do BrewApp (Jira BREW + git + build + docs/sprints). Usar quando o Jorge pedir review, retro, planning, fechamento ou ampliação de sprint.
argument-hint: "[nome ou id da sprint] [review | retro | planning | ampliacao] (padrão: sprint ativa, todas as fases)"
---

# Cerimônias de sprint do BrewApp

Roteiro reutilizável para qualquer sprint. Argumentos recebidos: `$ARGUMENTS`

- **Sprint:** nome (ex.: `Brew-02`) ou id numérico. Sem argumento, usar a sprint ativa (fase 0).
- **Fases:** sem indicação, rodar todas (review → retro → planning). Se vier `review`, `retro`, `planning` ou `ampliacao`, rodar só a fase pedida, mais a fase 0.

Regras que valem em todas as fases:

- Trabalhar em fases e **parar ao fim de cada uma** para o Jorge validar.
- **Nada é criado ou alterado no Jira sem aprovação explícita** (issue, pontos, sprint, descrição).
- **Nunca commitar, dar push ou abrir PR.** Ao final, listar os arquivos alterados e sugerir a mensagem de commit.
- Constantes do Jira (cloudId, campos de pontos e sprint, chaves das epics) estão no `CLAUDE.md`. Ler de lá, não repetir aqui.
- Respostas grandes do Jira estouram o limite do conector: pedir só os campos necessários e, se mesmo assim estourar, processar o arquivo salvo com PowerShell (`ConvertFrom-Json`).

## Fase 0: identificar a sprint

1. Com nome ou id: `sprint = "<nome>"` ou `sprint = <id>` em JQL.
2. Sem argumento: `project = BREW AND sprint in openSprints()`.
3. Ler `customfield_10020` de um dos itens para obter id, nome, estado e datas.
4. Localizar o documento da sprint em `docs/sprints/sprint-NN.md`. Se não existir, avisar antes de seguir.

## Fase 1: levantar os fatos

Coletar sem opinar ainda:

- **Plano:** ler `docs/sprints/sprint-NN.md` (compromisso, "se der", decisões tomadas durante a sprint).
- **Jira:** itens da sprint com `summary`, `issuetype`, `status`, `customfield_10016` (pontos), `timespent`, `parent`. Status de categoria Done (no BREW, "Production") significa mergeado na epic, ou seja, pronto pela Definition of Done.
- **Git:** `git log` das branches de epic envolvidas desde o início da sprint, merges de PR e mensagens com `#time`. Comparar as horas dos commits com o `timespent` do Jira: diferença costuma ser commit com prefixo `feat/` na mensagem (o Smart Commit não registra).
- **Build:** rodar `./mvnw -B verify` em `backend/` (precisa do Docker ligado; se estiver desligado, pedir para ligar em vez de pular). Se houver apps de front, rodar também lint, typecheck e testes deles.
- **Memória do projeto:** reler os pontos de atenção e pendências anotados (arquivos `project-*` da memória) para checar se algum venceu nesta sprint.

## Fase 2: review

Para cada item concluído:

1. Conferir o código contra os **critérios de aceite do Jira**, um a um.
2. Conferir a **Definition of Done** (`docs/processo/definition-of-done.md`) item a item, aceitando "não se aplica" com motivo.
3. Conferir se decisões tomadas durante o trabalho foram refletidas na spec, no design da API, nos ADRs e no próprio Jira.

Para itens não concluídos: registrar o motivo e o que falta.

Apresentar uma tabela por item (critérios ok? observações) e uma lista de **achados**, classificados como:

- **Bug:** comportamento errado ou critério de segurança/DoD violado. Propor issue do tipo Bug, com reprodução, impacto e critérios de aceite.
- **Task:** pendência de organização, documentação ou limpeza.
- **Ponto de atenção:** não é problema hoje, mas vai ser numa história futura. Propor anotar na memória do projeto, com a história em que deve ser retomado.

Para cada bug ou task proposto, sugerir epic, estimativa e posição no backlog. Não criar sem aprovação.

## Fase 3: retro

1. Preencher a seção **"Resultado"** do `sprint-NN.md`: concluído (sim/não, data), horas por item, observações.
2. Calcular e registrar:
   - pontos concluídos (velocidade da sprint);
   - horas totais;
   - horas por ponto (referência, não meta);
   - comparação com as sprints anteriores (média das últimas três, quando houver).
3. Apontar padrões nos números (ex.: pontos e horas descolados, tipo de história mais caro que o estimado).
4. Fazer ao Jorge as três perguntas da retro e registrar as respostas no documento:
   - o que funcionou;
   - o que atrapalhou;
   - o que muda na próxima sprint.
5. Registrar em "Decisões durante o sprint" qualquer decisão de processo tomada aqui.

## Fase 4: planning

1. **Capacidade:** perguntar as horas disponíveis na próxima sprint (feriados, viagens, outras prioridades).
2. **Velocidade de referência:** média dos pontos concluídos nas últimas três sprints (ou o que houver), planejando com 20 a 30% de folga.
3. **Candidatos:** seguir a ordem de dependências do backlog (epics na sequência do `CLAUDE.md`; dentro da epic, pré-requisitos primeiro), mais os bugs e tasks aprovados na review e os pontos de atenção da memória que vencem nas histórias candidatas.
4. **Estimativa:** itens sem pontos são estimados antes de entrar (Fibonacci, tamanho relativo; referência: BREW-12 = 2 pontos). Reestimar itens cujo escopo mudou.
5. **Proposta:** tabela com ordem, prioridade (compromisso ou "se der"), item, pontos e motivo da posição; total de compromisso e de "se der"; objetivo da sprint em uma frase; riscos e mitigações.
6. Após a aprovação:
   - o Jorge cria a sprint no Jira (o conector não cria sprints);
   - descobrir o id da sprint nova pelo `customfield_10020` de um item que o Jorge mover para ela, ou perguntar o número da URL do board;
   - mover os itens aprovados (`customfield_10020` = id);
   - criar `docs/sprints/sprint-NN.md` a partir do modelo abaixo;
   - atualizar a seção "Sprint atual" do `CLAUDE.md` (nome, id, datas, objetivo, ordem).

## Variação: ampliação no meio da sprint

Quando o compromisso termina antes do prazo, a recomendação padrão é **ampliar a sprint** em vez de encerrá-la, para a velocidade continuar comparável:

1. Fazer a review (fase 2) do que já foi concluído.
2. Propor os próximos itens do backlog, na ordem de dependências, separando compromisso e "se der".
3. Após a aprovação, mover os itens e registrar no `sprint-NN.md` uma seção "Ampliação em DD/MM", com a tabela de ordem e motivo, e a decisão em "Decisões durante o sprint".
4. A retro e a velocidade final ficam para a data de fim da sprint.

## Encerramento

- Listar os arquivos alterados (normalmente `docs/sprints/sprint-NN.md`, o novo `sprint-NN.md` e o `CLAUDE.md`).
- Sugerir a mensagem de commit no padrão `BREW-<n> [Descrição em pt-BR]`, usando a chave do primeiro item da próxima fila de trabalho. Sem `#time`, salvo se o Jorge quiser contabilizar o tempo da cerimônia.
- Lembrar o Jorge de iniciar a sprint no Jira, colando o objetivo no campo de meta.

## Modelo de `docs/sprints/sprint-NN.md`

```markdown
# Sprint N (Brew-NN)

- **Período:** DD/MM/AAAA a DD/MM/AAAA
- **Feriados no período:** (ou "nenhum")
- **Capacidade estimada:** X horas
- **Velocidade de referência:** X pontos (média das últimas N sprints)

## Objetivo

(uma frase)

## Backlog da sprint

| Ordem | Prioridade | Item | Pontos | Motivo da posição |
|---|---|---|---|---|

- **Compromisso:** X pontos
- **Se der:** X pontos

## Riscos

| Risco | Mitigação |
|---|---|

## Cerimônias

| Data | Evento |
|---|---|
| DD/MM | Planning |
| DD/MM | Checagem de meio de sprint |
| DD/MM | Review e retro |

## Decisões durante o sprint

| Data | Decisão |
|---|---|

## Resultado (preencher na retro)

| Item | Pontos | Concluído? | Horas gastas | Observações |
|---|---|---|---|---|

- **Pontos concluídos (velocidade da sprint):**
- **Horas totais:**
- **Horas por ponto (referência, não meta):**
- **Comparação com as sprints anteriores:**

### Retro

- **O que funcionou:**
- **O que atrapalhou:**
- **O que muda no próximo sprint:**
```
