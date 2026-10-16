# BrewApp

Diário de adega para cervejarias: acompanha cada lote do mosto resfriado até o momento anterior ao envase, com registro no celular (inclusive sem conexão) e comparação entre lotes.

> **In English:** BrewApp is a cellar log for breweries. It tracks each batch from cooled wort to packaging, with offline-first data entry on mobile and side-by-side batch comparison. It is a personal and portfolio project, built as a modular monolith in Java 25 and Spring Boot 4, with PostgreSQL, Keycloak, React, Tauri and React Native. The documentation is written in Brazilian Portuguese.

## O problema

O planejamento de receitas já é bem atendido por ferramentas consolidadas. A lacuna está depois da brassagem: leituras de densidade e temperatura, dry hop, purgas de levedura, clarificação e carbonatação ainda são anotadas no papel e depois passadas para planilhas. Isso gera retrabalho, perde informação e torna difícil responder a pergunta que mais importa para manter um padrão: por que este lote ficou diferente do anterior?

O BrewApp troca o papel por um diário estruturado, em que cada intervenção é um registro com tipo, data, quantidade e responsável. A partir desse diário, é possível sobrepor as curvas de fermentação de vários lotes pelo tempo decorrido desde a inoculação e enxergar onde eles se afastam.

## Status

MVP em construção. A fundação técnica está pronta: backend, banco de dados, autenticação, logs e regras de arquitetura verificadas no build. O app web e desktop já tem a sua base (roteamento, cliente da API e a casca desktop com Tauri). As funcionalidades de negócio e o aplicativo mobile ainda serão desenvolvidos.

O MVP é para uso pessoal, mas o modelo de dados e a autorização já nascem preparados para várias organizações e usuários com papéis diferentes.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 25, Spring Boot 4, Spring Modulith, Spring Security, Maven |
| Dados | PostgreSQL 18, Flyway, jOOQ |
| Autenticação | Keycloak (OpenID Connect com PKCE) |
| Web e desktop | React, TypeScript, Vite e Tauri |
| Mobile | React Native (Expo), com SQLite local para o modo offline |
| Monorepo | pnpm workspaces |

## Arquitetura em poucas palavras

O backend é um monolito modular. Cada módulo de negócio (organização, recipientes, lotes, diário de adega e comparação) tem o seu próprio schema no banco e só conversa com os outros pelas interfaces públicas, numa direção única de dependência. Dentro de cada módulo, o domínio fica isolado de framework e banco, no estilo hexagonal. Essas fronteiras são verificadas automaticamente nos testes, então uma violação quebra o build.

O isolamento entre organizações é feito no próprio banco, com Row Level Security, e a aplicação se conecta com uma role que não é dona das tabelas. Os eventos do diário são imutáveis: um registro errado é corrigido por uma retificação, nunca editado ou apagado.

Os detalhes e os motivos de cada decisão estão na spec do MVP e nos ADRs (links no fim desta página).

## Estrutura do repositório

| Pasta | Conteúdo |
|---|---|
| `backend/` | API em Spring Boot, migrations e código gerado pelo jOOQ |
| `apps/web/` | App web em React; a casca desktop (Tauri) fica em `apps/web/src-tauri/` |
| `apps/mobile/` | App mobile (em breve) |
| `packages/shared/` | Tipos da API e catálogo de unidades compartilhados entre os apps; as traduções entram em breve |
| `infra/` | PostgreSQL e Keycloak para desenvolvimento local, via Docker Compose |
| `docs/` | Spec, design da API, processo, ADRs e planos de sprint |

## Como rodar localmente

### Pré-requisitos

- JDK 25 (o Maven não precisa ser instalado: o projeto traz o Maven Wrapper)
- Docker com Compose v2, como o Docker Desktop
- Node.js 24 ou mais recente e pnpm (a versão usada está fixada no `package.json` da raiz; com o Corepack ativo, ela é instalada sozinha)
- gitleaks, para a varredura de segredos antes de cada commit (no Windows: `winget install Gitleaks.Gitleaks`)
- Só para o app desktop: Rust e, no Windows, as ferramentas de compilação C++ da Microsoft (veja o passo 7)

### 1. Instalar as dependências e ativar o hook de commit

Na raiz do repositório:

```bash
pnpm install
```

Além de instalar as dependências, o `pnpm install` ativa o hook de pre-commit que roda o gitleaks e bloqueia o commit se encontrar algo parecido com um segredo. Ele faz isso apontando o Git para a pasta `.githooks`, no script `prepare` do `package.json`.

O pnpm só roda esse script quando a instalação tem algo a fazer. Se as dependências já estavam instaladas, ou se você não for usar o pnpm, ative o hook manualmente:

```bash
git config core.hooksPath .githooks
```

Para conferir se o hook está ativo, o comando `git config core.hooksPath` deve responder `.githooks`. Sem o gitleaks instalado, o hook bloqueia o commit em vez de deixá-lo passar sem a varredura.

### 2. Criar o `.env`

Copie o `.env.example` da raiz para `.env` e troque todos os valores marcados com `troque-me`. O `.env` fica fora do Git e é lido tanto pelo Docker Compose quanto pelo backend.

### 3. Subir o PostgreSQL e o Keycloak

Na pasta `infra/`:

```bash
docker compose up -d --wait
```

A primeira subida leva cerca de um minuto, porque o Keycloak importa o realm do projeto. Endereços, usuários, reset do ambiente e limitações conhecidas estão no [README da infraestrutura](infra/README.md).

### 4. Rodar o backend

O backend lê toda a configuração de variáveis de ambiente, sem valores padrão: se faltar alguma, ele não sobe.

**Pelo IntelliJ IDEA:** crie uma run configuration do Spring Boot para a classe `BrewAppApplication` e aponte o arquivo `.env` da raiz como fonte das variáveis de ambiente.

**Pelo terminal (Git Bash, Linux ou macOS):** na pasta `backend/`, carregue o `.env` e suba a aplicação com o Maven Wrapper:

```bash
set -a; source ../.env; set +a
./mvnw spring-boot:run
```

Na subida, o Flyway aplica as migrations. A API fica em `http://localhost:8080`, ou na porta definida em `BREWAPP_SERVER_PORT`.

### 5. Rodar os testes

Na pasta `backend/`, com o Docker rodando:

```bash
./mvnw verify
```

Os testes de persistência usam um PostgreSQL real em container (Testcontainers), e não um banco em memória. Por isso o Docker precisa estar ativo. Os testes não dependem do `.env` nem do ambiente do `infra/`.

### 6. Formatação do código Java

O backend segue o Palantir Java Format, aplicado pelo Spotless só nos arquivos alterados. Arquivos que ninguém mexeu continuam como estão. Não é preciso rodar nada à parte:

- **Pelo Maven:** todo build (`compile`, `test`, `verify`) formata os arquivos alterados desde o último commit.
- **Pelo IntelliJ IDEA:** instale o plugin palantir-java-format, habilite-o nas configurações e, em Tools → Actions on Save, marque "Reformat code" e "Optimize imports" para arquivos Java. Assim o arquivo é formatado ao ser salvo.

Na pull request, a CI confere a formatação dos arquivos alterados em relação à branch de destino. Se ela reclamar de algo que já foi commitado, formate a partir da pasta `backend/`, informando a branch de destino, e commite o resultado:

```bash
./mvnw spotless:apply -Dformatacao.base=origin/epic/BREW-5
```

### 7. Rodar o app web e o desktop

Na raiz do repositório, o app web sobe em `http://localhost:5173`:

```bash
pnpm --filter @brewapp/web dev
```

O desktop abre o mesmo app numa janela própria, servido em `http://127.0.0.1:1420`. As duas portas são fixas porque estão cadastradas no Keycloak e no CORS do backend. Antes da primeira vez, no Windows, instale as ferramentas de compilação C++ e o Rust; depois, abra um terminal novo para o PATH ser atualizado:

```bash
winget install --id Microsoft.VisualStudio.2022.BuildTools --source winget --override "--passive --wait --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended"
winget install --id Rustlang.Rustup --source winget
```

Com isso instalado, na raiz do repositório:

```bash
pnpm --filter @brewapp/web tauri dev
```

A primeira execução compila as dependências do Rust e leva alguns minutos; as seguintes são rápidas. Mudanças no código do app web aparecem na janela sem recompilar o Rust.

Os testes do app rodam com `pnpm --filter @brewapp/web test`, sem Docker nem backend.

### 8. Lint e formatação do TypeScript

O ESLint e o Prettier são configurados na raiz e valem para o app web e para o `packages/shared`. O lint proíbe `any` e casts forçados: tipo errado se corrige na definição. Para conferir tudo e para formatar, na raiz:

```bash
pnpm lint
pnpm formatar
```

Cada pacote também tem os seus `lint` e `formatar`, que a CI usa (por exemplo, `pnpm --filter @brewapp/web lint`).

## Integração contínua

Toda pull request e todo push para a `main` e para as branches de epic passam pelo GitHub Actions: build e testes do backend, regras de arquitetura, formatação, conferência do código gerado pelo jOOQ, lint, testes e build do app web, varredura de segredos, dependências vulneráveis e análise estática com o CodeQL. O merge na `main` só é liberado com tudo verde. O Dependabot propõe as atualizações de dependências toda semana. Os detalhes estão no [fluxo de branches](docs/processo/fluxo-de-branches.md).

## Documentação

- [Spec do MVP](docs/specs/2026-10-03-mvp-diario-adega-design.md): domínio, arquitetura, dados, offline, segurança e testes
- [Design da API](docs/api/design-api.md): convenções e endpoints
- [Fluxo de branches](docs/processo/fluxo-de-branches.md): branches, commits e merges
- [Definition of Done](docs/processo/definition-of-done.md): quando uma entrega está pronta
- [ADRs](docs/adr/): registros das decisões de arquitetura
- [Sprints](docs/sprints/): plano e resultado de cada sprint

## Licença

Este repositório é público para consulta, mas ainda não tem uma licença de código aberto. Todos os direitos reservados.
