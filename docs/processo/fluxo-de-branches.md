# Fluxo de branches

## Estrutura

```
main  ◀──  epic/BREW-5  ◀──  feat/BREW-12
                        ◀──  feat/BREW-13
      ◀──  epic/BREW-6  ◀──  feat/BREW-23
```

- **`main`:** o que está em produção. Só recebe merges de branches de epic.
- **`epic/BREW-<n>`:** uma por epic do Jira. Recebe as branches de trabalho das histórias daquela epic. Quando a epic está pronta, ela é mergeada na `main`, o que dispara o deploy.
- **Branches de trabalho:** uma por item do Jira (história, task ou bug), criada a partir da branch da epic e mergeada de volta nela.

Como o projeto tem um único desenvolvedor, só uma epic e uma branch de trabalho estão em andamento por vez. A próxima epic sai da `main` depois que a anterior foi mergeada.

## Nomenclatura

| Branch | Padrão | Exemplo |
|---|---|---|
| Epic | `epic/BREW-<n>` | `epic/BREW-5` |
| História ou task | `feat/BREW-<n>` | `feat/BREW-22` |
| Bug | `hotfix/BREW-<n>` | `hotfix/BREW-70` |

A chave do Jira deve aparecer **exatamente** como no Jira (`BREW-5`, nunca `BREW-05`). É assim que o Jira vincula branches, commits e pull requests à issue.

### Origem das branches de bug

- **Bug encontrado durante o trabalho de uma epic** (ainda não chegou à `main`): `hotfix/BREW-<n>` sai da branch da epic e volta para ela.
- **Bug em produção** (já está na `main`): `hotfix/BREW-<n>` sai da `main` e volta para a `main`. Em seguida, a `main` é mergeada na branch da epic em andamento, para que a correção não se perca.

### Sincronizar a `main` na epic

A sincronização é feita com merge direto, sem pull request:

```bash
git checkout epic/BREW-5
git merge main
git push
```

A CI roda no push para a branch da epic, então a sincronização é verificada logo em seguida. As branches de trabalho continuam entrando na epic por pull request.

## Merges

- **Somente merge commit.** Squash e rebase estão desabilitados no repositório.
- Motivo: o Jira registra o `#time` de cada commit pelo seu hash. Squash e rebase criam commits novos e repetem as mensagens com `#time`, o que duplica as horas registradas. O merge commit preserva os commits originais.
- Commits com `#time` que já foram enviados ao GitHub não devem ser reescritos (`amend`, `rebase`), pelo mesmo motivo.

## Quando uma história está pronta

Uma história, task ou bug está **pronto** quando é mergeado na branch da epic com a CI verde. A chegada da epic à `main` é uma entrega de release, não uma condição para concluir histórias no sprint.

## Commits

Padrão: `tipo/BREW-<n> #time <tempo> [Descrição em pt-BR]`

Exemplo: `feat/BREW-12 #time 2h [Estrutura inicial do monorepo]`

O `#time` é um Smart Commit: o horário é registrado na issue do Jira quando o commit é enviado ao GitHub.

## Integração contínua

- CI completa em toda pull request para `main` e para `epic/**`, e em push nessas branches.
- Proteções (rulesets do GitHub):

  | Ruleset | Alvo | Regras |
  |---|---|---|
  | `protecao-main` | `main` | Exige pull request (somente merge commit) e CI verde; bloqueia force push e exclusão |
  | `protecao-epics` | `epic/**` | Bloqueia force push e exclusão; **não** exige pull request, para permitir a sincronização direta com a `main` |

- A regra "exigir histórico linear" fica desligada em ambos, porque é incompatível com merge commit.
- Por convenção, as branches de trabalho entram na epic sempre por pull request, mesmo sem o GitHub exigir.
- Deploy apenas no merge para a `main`.
