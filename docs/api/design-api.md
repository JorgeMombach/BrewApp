# BrewApp — Design da API

- **Status:** seções 1 a 6 aprovadas; seção 7 em revisão.
- **Base:** spec do MVP (`docs/specs/2026-10-03-mvp-diario-adega-design.md`)

Este documento desenha a API **antes** do código. Depois, o OpenAPI gerado pelo backend (Swagger) precisa corresponder ao que está descrito aqui. Quando os dois divergirem, ou o código está errado, ou este documento precisa ser atualizado de forma consciente.

Ordem de desenho:

1. Convenções gerais
2. Identidade e organização
3. Recipientes
4. Lotes
5. Diário de adega
6. Sincronização (mobile)
7. Comparação

---

## 1. Convenções gerais

Estas regras valem para todos os endpoints, sem exceção.

### 1.1 Endereço e versão

- Todas as rotas começam com `/api/v1`.
- A versão fica no caminho. Uma mudança incompatível gera `/api/v2`, convivendo com a anterior durante a transição. Mudanças compatíveis (um campo novo opcional, um endpoint novo) não mudam a versão.
- Nomes de recursos em português, no plural e em `kebab-case` (ex.: `/recipientes`, `/eventos-adega`), acompanhando a linguagem do domínio.

### 1.2 Cabeçalhos

| Cabeçalho | Direção | Obrigatório | Uso |
|---|---|---|---|
| `Authorization: Bearer <token>` | requisição | sempre, exceto `/actuator/health` | token emitido pelo Keycloak |
| `X-Organization-Id` | requisição | em todo recurso de negócio | organização em uso; validada contra os vínculos do usuário |
| `X-Correlation-Id` | requisição e resposta | não | se enviado, é reaproveitado; se não, o servidor gera um UUID. Sempre volta na resposta. Formato aceito: 1 a 64 caracteres entre letras, dígitos, `.`, `_` e `-`; fora disso, o valor enviado é descartado e o servidor gera outro (o identificador vai para os logs) |
| `Accept-Language` | requisição | não | **ignorado pelo backend** para mensagens (o cliente traduz); usado apenas como sugestão de idioma no primeiro acesso |
| `ETag` / `If-Match` | resposta / requisição | `If-Match` em toda alteração de recurso mutável | controle de concorrência (ver 1.7) |

**Regra do tenant:** a organização vem **somente** do cabeçalho `X-Organization-Id`, nunca do corpo nem de parâmetros. Se um corpo de requisição trouxer um campo de organização, ele é ignorado.

### 1.3 Formato dos dados

- JSON com propriedades em `camelCase`.
- **Identificadores:** UUIDv7 como texto.
- **Datas e horas:** ISO-8601 com fuso explícito (ex.: `2026-10-03T14:30:00Z`). O servidor sempre responde em UTC; a conversão para o fuso do usuário é feita no cliente.
- **Valores de domínio** (status, tipos, papéis, off-flavors): códigos em `UPPER_SNAKE_CASE` (ex.: `FERMENTANDO`). Nunca texto traduzido.
- **Quantidades:** sempre como objeto com valor e unidade, nunca como texto (ex.: `{ "valor": 50, "unidade": "G" }`).
- **Números decimais:** enviados como número JSON, sempre com ponto decimal. A conversão de vírgula é responsabilidade da interface.
- Campos ausentes e campos `null` têm o mesmo significado: "sem valor".

### 1.4 Paginação

**Toda listagem é paginada.** Não existe endpoint que devolva uma coleção inteira.

- Paginação por *keyset* com cursor opaco.
- Parâmetros: `limite` (padrão 50, máximo 200) e `cursor` (o valor recebido na página anterior).
- Resposta:

```json
{
  "itens": [ ... ],
  "proximoCursor": "eyJpZCI6IjAxOTI..."
}
```

- `proximoCursor` nulo significa que não há mais páginas.
- O total de registros **não** é devolvido por padrão, porque contar a coleção inteira custa caro em tabelas grandes. Se uma tela precisar do total, isso vira um endpoint específico, justificado.
- O cursor é opaco para o cliente: ele nunca deve interpretá-lo ou montá-lo.

### 1.5 Filtros e ordenação

- Filtros como parâmetros de consulta com nomes explícitos (ex.: `?status=FERMENTANDO&recipienteId=...`).
- A ordenação de cada listagem é **fixa e documentada** por endpoint, para que sempre exista um índice que a sustente. Ordenação livre por qualquer campo não é oferecida.
- Inativos são excluídos por padrão. Para incluí-los, o parâmetro explícito é `incluirInativos=true`.

### 1.6 Erros

Todos os erros seguem o formato **Problem Details (RFC 9457)**, estendido com um código de erro estável, que o cliente usa para buscar a mensagem traduzida.

```json
{
  "type": "https://brewapp.dev/erros/lote-envasado",
  "title": "Lote já envasado",
  "status": 409,
  "code": "LOTE_ENVASADO",
  "params": { "loteId": "0192...", "envasadoEm": "2026-10-01T10:00:00Z" },
  "correlationId": "4f1c...",
  "errors": []
}
```

- `code` é o contrato. O `title` existe só para leitura humana em logs e ferramentas; **o cliente nunca exibe o `title`**, e sim a tradução do `code`.
- `correlationId` é o mesmo valor do cabeçalho `X-Correlation-Id` da resposta. Vem em todo erro, inclusive nos 401 e 403 da camada de segurança, para que o usuário possa informá-lo ao relatar um problema.
- Em erros de validação, `errors` lista cada campo com problema: `{ "campo": "capacidade.valor", "code": "DEVE_SER_POSITIVO", "params": {} }`.
- Nenhuma resposta de erro contém stack trace, nome de classe, SQL ou detalhe interno.

**Códigos HTTP:**

| Status | Quando |
|---|---|
| 400 | Corpo malformado ou falha de validação de formato |
| 401 | Token ausente, inválido ou expirado |
| 403 | Usuário autenticado sem a permissão necessária, ou sem vínculo com a organização do cabeçalho |
| 404 | Recurso não existe **ou pertence a outra organização** |
| 405 | Método HTTP não suportado na rota (a resposta traz o cabeçalho `Allow`) |
| 406 | O `Accept` pedido não pode ser atendido |
| 409 | Conflito com o estado atual (ex.: nome duplicado, lote em estado que não aceita a operação) |
| 412 | `If-Match` não corresponde à versão atual do recurso |
| 415 | `Content-Type` não suportado |
| 422 | Corpo bem formado, mas viola uma regra de domínio |
| 428 | Alteração de recurso mutável sem `If-Match` |
| 429 | Limite de requisições excedido |
| 500 | Erro inesperado no servidor (o detalhe fica só no log) |

**Recurso de outra organização responde 404, não 403.** Responder 403 confirmaria que o recurso existe, e isso já é vazamento de informação.

**Códigos genéricos.** Valem para qualquer endpoint e não dependem do domínio. Os códigos de negócio (como `LOTE_ENVASADO`) são documentados junto de cada endpoint.

| `code` | Status | Quando |
|---|---|---|
| `VALIDACAO_FALHOU` | 400 | Um ou mais campos ou parâmetros violam as regras de formato; o detalhe de cada um vem em `errors` |
| `CORPO_MALFORMADO` | 400 | O corpo não é um JSON válido ou não pode ser lido |
| `REQUISICAO_INVALIDA` | 400 | Outros problemas da requisição, como parâmetro obrigatório ausente ou valor de tipo errado na URL |
| `NAO_AUTENTICADO` | 401 | Token ausente, inválido ou expirado |
| `ACESSO_NEGADO` | 403 | Falta a permissão necessária |
| `ROTA_NAO_ENCONTRADA` | 404 | A rota não existe (diferente de um recurso não encontrado, que tem código próprio) |
| `METODO_NAO_SUPORTADO` | 405 | Método HTTP não suportado na rota |
| `TIPO_DE_RESPOSTA_NAO_SUPORTADO` | 406 | O `Accept` pedido não pode ser atendido |
| `TIPO_DE_CONTEUDO_NAO_SUPORTADO` | 415 | `Content-Type` não suportado |
| `ERRO_INTERNO` | 500 | Erro inesperado no servidor |

**Códigos dos erros de validação** (o `code` de cada item de `errors`). Os parâmetros da regra vêm em `params`, com o nome usado na regra: o limite de um `ACIMA_DO_MAXIMO` chega como `{ "value": 200 }`, e o de um `TAMANHO_INVALIDO` como `{ "min": 1, "max": 100 }`.

| `code` | Quando |
|---|---|
| `OBRIGATORIO` | Campo ausente, nulo ou em branco |
| `DEVE_SER_POSITIVO` | Valor precisa ser maior que zero |
| `NAO_PODE_SER_NEGATIVO` | Valor precisa ser zero ou mais |
| `TAMANHO_INVALIDO` | Texto ou lista fora do tamanho permitido |
| `ABAIXO_DO_MINIMO` | Valor abaixo do mínimo |
| `ACIMA_DO_MAXIMO` | Valor acima do máximo |
| `FORMATO_INVALIDO` | Texto fora do formato esperado (ex.: e-mail) |
| `VALOR_INVALIDO` | Valor de tipo errado para o campo (ex.: texto num campo numérico) |

Uma regra de validação nova que não esteja nesta tabela usa o nome da regra em `UPPER_SNAKE_CASE` (por exemplo, `DECIMAL_MIN`) até ganhar um código próprio aqui.

### 1.7 Concorrência (lock otimista)

Recursos mutáveis (organização, recipiente, lote) usam o mecanismo padrão do HTTP:

1. Toda leitura de um recurso mutável devolve o cabeçalho `ETag` com a versão atual.
2. Toda alteração (`PATCH`, ações de estado) exige `If-Match` com essa versão.
3. Se a versão mudou nesse meio tempo, a resposta é **412**, e o cliente recarrega antes de tentar de novo.
4. Se o `If-Match` não vier, a resposta é **428**, o que impede sobrescrever sem querer.

Eventos de adega não usam esse mecanismo, porque são imutáveis.

### 1.8 Criação, alteração e inativação

- **Criação:** `POST` na coleção. Resposta **201** com o recurso criado no corpo e o cabeçalho `Location`.
- **Alteração parcial:** `PATCH` com apenas os campos alterados (JSON Merge Patch, RFC 7396).
- **Não existe `DELETE` de dados de negócio.** Inativação e reativação são ações explícitas, para que ninguém confunda "inativar" com "apagar":
  - `POST /recurso/{id}/inativacao`
  - `POST /recurso/{id}/reativacao`
- **Mudanças de estado** (como no ciclo de vida do lote) também são ações explícitas, nunca um `PATCH` no campo de status. Assim cada transição tem sua própria permissão e suas próprias regras.

### 1.9 Idempotência

- `GET`, `PATCH` com `If-Match` e as ações de estado são naturalmente seguros contra repetição: uma segunda tentativa recebe 412 ou 409.
- A criação de eventos de adega usa o **identificador gerado pelo cliente** como chave de idempotência: reenviar o mesmo evento não o duplica (detalhado na seção de sincronização).

### 1.10 Limite de requisições

- Limite por usuário, com valores mais restritos na sincronização.
- Ao exceder: resposta **429** com o cabeçalho `Retry-After`.

### 1.11 Unidades de medida

Toda grandeza aceita várias unidades, porque cada cervejaria usa o seu padrão. As conversões ficam num **catálogo de unidades**: cada unidade tem sua conversão para a unidade canônica e seus casos de teste no arquivo compartilhado entre Java e TypeScript. Incluir uma unidade nova é acrescentar uma entrada ao catálogo, sem mudar regras de domínio.

| Grandeza | Unidades aceitas | Canônica |
|---|---|---|
| Densidade | `SG`, `PLATO`, `BRIX`, `G_CM3` | SG |
| Temperatura | `CELSIUS`, `FAHRENHEIT` | °C |
| Pressão | `BAR`, `PSI`, `KPA` | bar |
| Volume | `L`, `HL`, `ML`, `GAL_US`, `GAL_UK`, `BBL_US` | L |
| Massa | `G`, `KG`, `OZ`, `LB` | g |

- `G_CM3` é convertido para SG usando a densidade da água a 20 °C como referência.
- `BBL_US` é o barril de cerveja americano (117,348 L), comum em equipamentos de cervejaria artesanal.
- A API sempre devolve o valor informado (com sua unidade) e o valor canônico. A conversão para a unidade preferida do usuário é feita no cliente.

---

## 2. Identidade e organização

### 2.1 Primeiro acesso

O usuário se cadastra e faz login **no Keycloak**, nunca na API do BrewApp. Na primeira chamada autenticada, a API cria automaticamente o registro local do usuário a partir do token: o identificador do provedor (`sub`), o nome e o e-mail. Não existe endpoint de cadastro de usuário.

Logo após o primeiro login, o usuário ainda não tem nenhuma organização. O fluxo do MVP é:

1. O cliente chama `GET /api/v1/me` e recebe a lista de vínculos vazia.
2. O cliente mostra a tela "criar sua cervejaria".
3. `POST /api/v1/organizacoes` cria a organização e torna o usuário `OWNER` dela.

### 2.2 Permissões e papéis

As permissões são o contrato. Os papéis são apenas agrupamentos de permissões. Cada endpoint declara **uma** permissão.

| Permissão | OWNER | CERVEJEIRO | OPERADOR | LEITOR |
|---|:-:|:-:|:-:|:-:|
| `ORGANIZACAO_LER` | ✓ | ✓ | ✓ | ✓ |
| `ORGANIZACAO_EDITAR` | ✓ | | | |
| `MEMBRO_LISTAR` | ✓ | ✓ | | |
| `RECIPIENTE_LER` | ✓ | ✓ | ✓ | ✓ |
| `RECIPIENTE_GERENCIAR` | ✓ | ✓ | | |
| `LOTE_LER` | ✓ | ✓ | ✓ | ✓ |
| `LOTE_GERENCIAR` | ✓ | ✓ | | |
| `LOTE_MUDAR_ESTADO` | ✓ | ✓ | | |
| `EVENTO_LER` | ✓ | ✓ | ✓ | ✓ |
| `EVENTO_REGISTRAR` | ✓ | ✓ | ✓ | |
| `EVENTO_RETIFICAR` | ✓ | ✓ | ✓ | |
| `COMPARACAO_LER` | ✓ | ✓ | ✓ | ✓ |

No MVP só existe o papel `OWNER` em uso, mas a tabela já fica definida para que nenhum endpoint seja criado sem permissão associada.

### 2.3 Endpoints

#### `GET /api/v1/me`

Devolve o usuário autenticado, suas preferências e seus vínculos. Não exige `X-Organization-Id`.

**Permissão:** apenas autenticação.

```json
{
  "id": "0192f3a1-...",
  "nome": "Jorge Mombach",
  "email": "jorge@exemplo.com",
  "preferencias": {
    "idioma": "pt-BR",
    "fusoHorario": "America/Sao_Paulo",
    "unidadeDensidade": "BRIX",
    "unidadeTemperatura": "CELSIUS",
    "unidadePressao": "BAR",
    "unidadeVolume": "L"
  },
  "vinculos": [
    {
      "organizacaoId": "0192f3a2-...",
      "organizacaoNome": "Cervejaria do Jorge",
      "papel": "OWNER"
    }
  ]
}
```

Os vínculos não são paginados. Um usuário tem, na prática, poucos vínculos, e este endpoint é chamado no início de toda sessão. Esta é a única exceção à regra de paginação, e por isso vale um ADR.

#### `PATCH /api/v1/me/preferencias`

Altera as preferências do próprio usuário. As preferências pertencem só ao usuário e não sofrem edição concorrente, por isso este endpoint não exige `If-Match`.

**Permissão:** apenas autenticação.

```json
{ "unidadeDensidade": "PLATO", "idioma": "es" }
```

| Campo | Valores aceitos |
|---|---|
| `idioma` | `pt-BR`, `es`, `en` |
| `fusoHorario` | identificador IANA (ex.: `America/Sao_Paulo`) |
| `unidadeDensidade` | qualquer unidade de densidade do catálogo (1.11) |
| `unidadeTemperatura` | qualquer unidade de temperatura do catálogo (1.11) |
| `unidadePressao` | qualquer unidade de pressão do catálogo (1.11) |
| `unidadeVolume` | qualquer unidade de volume do catálogo (1.11) |

**Resposta:** 200 com as preferências completas. **Erros:** 400 (valor fora da lista).

#### `POST /api/v1/organizacoes`

Cria uma organização e torna o usuário autenticado seu `OWNER`.

**Permissão:** apenas autenticação. No MVP, cada usuário pode ser `OWNER` de no máximo **uma** organização, o que evita abuso enquanto não existe o cenário SaaS. O limite é configurável.

```json
{ "nome": "Cervejaria do Jorge" }
```

**Resposta:** 201 com a organização e `Location: /api/v1/organizacoes/{id}`.
**Erros:** 400 (nome vazio ou acima de 120 caracteres), 409 `LIMITE_ORGANIZACOES_ATINGIDO`.

#### `GET /api/v1/organizacoes/{organizacaoId}`

Dados da organização. Aqui a organização vem do caminho, porque o recurso é a própria organização. Mesmo assim, o vínculo do usuário com ela é verificado.

**Permissão:** `ORGANIZACAO_LER`.

```json
{
  "id": "0192f3a2-...",
  "nome": "Cervejaria do Jorge",
  "criadaEm": "2026-10-03T14:30:00Z"
}
```

Com cabeçalho `ETag`. **Erros:** 404 (inexistente ou sem vínculo).

#### `PATCH /api/v1/organizacoes/{organizacaoId}`

Altera os dados da organização.

**Permissão:** `ORGANIZACAO_EDITAR`. **Exige** `If-Match`.

```json
{ "nome": "Cervejaria Mombach" }
```

**Resposta:** 200 com o recurso atualizado e o novo `ETag`. **Erros:** 400, 403, 404, 412, 428.

#### `GET /api/v1/organizacoes/{organizacaoId}/membros`

Lista os membros da organização, paginado e ordenado por nome.

**Permissão:** `MEMBRO_LISTAR`.

```json
{
  "itens": [
    {
      "usuarioId": "0192f3a1-...",
      "nome": "Jorge Mombach",
      "papel": "OWNER",
      "vinculadoEm": "2026-10-03T14:30:00Z"
    }
  ],
  "proximoCursor": null
}
```

### 2.4 Fora do MVP

- Convites, inclusão e remoção de membros, troca de papel. São do cenário multiusuário/SaaS e entram junto com as telas de administração.
- Exclusão e anonimização da própria conta pelo usuário. A anonimização está prevista no modelo de dados (LGPD), mas o fluxo de solicitação entra com o cenário multiusuário, porque exige regras como "o único `OWNER` de uma organização não pode sair sem transferir a posse".
- Troca de organização em uso não é um endpoint: o cliente escolhe a organização e passa a enviar outro valor no `X-Organization-Id`.

---

## 3. Recipientes

Cadastro dos fermentadores, maturadores e demais tanques. Todos os endpoints exigem `X-Organization-Id`.

### 3.1 Recurso

```json
{
  "id": "0192f3b0-...",
  "nome": "FV-01",
  "tipo": "FERMENTADOR",
  "capacidade": { "valor": 500, "unidade": "L" },
  "observacao": "Cônico com camisa de glicol",
  "ocupacao": {
    "loteId": "0192f3c4-...",
    "loteCodigo": "L-2026-014",
    "desde": "2026-09-28T09:15:00Z"
  },
  "inativadoEm": null,
  "criadoEm": "2026-10-03T14:30:00Z"
}
```

| Campo | Regras |
|---|---|
| `nome` | obrigatório, 1 a 60 caracteres, único entre os recipientes **ativos** da organização |
| `tipo` | `FERMENTADOR`, `MATURADOR` (inclui BBT) ou `OUTRO` |
| `capacidade` | obrigatória; valor maior que zero; qualquer unidade de volume do catálogo (1.11). O banco guarda também o valor canônico em litros |
| `observacao` | opcional, até 500 caracteres |
| `ocupacao` | somente leitura; `null` quando o recipiente está vazio (ver 3.3) |
| `inativadoEm` | somente leitura; `null` quando ativo |

### 3.2 Endpoints

#### `GET /api/v1/recipientes`

Lista paginada, **ordenada por nome** (e por id como desempate, para o cursor ser estável).

**Permissão:** `RECIPIENTE_LER`.

| Parâmetro | Uso |
|---|---|
| `tipo` | filtra por tipo |
| `ocupado` | `true` ou `false` |
| `incluirInativos` | padrão `false` |
| `limite`, `cursor` | paginação |

Índice que sustenta a consulta padrão: `(organization_id, nome, id) WHERE inativado_em IS NULL`.

#### `GET /api/v1/recipientes/{recipienteId}`

**Permissão:** `RECIPIENTE_LER`. Resposta com `ETag`. **Erros:** 404.

#### `POST /api/v1/recipientes`

**Permissão:** `RECIPIENTE_GERENCIAR`.

```json
{
  "nome": "FV-01",
  "tipo": "FERMENTADOR",
  "capacidade": { "valor": 500, "unidade": "L" },
  "observacao": "Cônico com camisa de glicol"
}
```

**Resposta:** 201 com o recurso e `Location`.
**Erros:** 400 (validação), 409 `NOME_RECIPIENTE_DUPLICADO`.

#### `PATCH /api/v1/recipientes/{recipienteId}`

Altera nome, tipo, capacidade ou observação.

**Permissão:** `RECIPIENTE_GERENCIAR`. **Exige** `If-Match`.

**Erros:** 400, 404, 409 `NOME_RECIPIENTE_DUPLICADO`, 412, 428, e 422 `RECIPIENTE_INATIVO` (um recipiente inativo precisa ser reativado antes de ser alterado).

#### `POST /api/v1/recipientes/{recipienteId}/inativacao`

**Permissão:** `RECIPIENTE_GERENCIAR`. **Exige** `If-Match`. Sem corpo.

**Resposta:** 200 com o recurso atualizado.
**Erros:** 404, 409 `RECIPIENTE_OCUPADO` (não é possível inativar um tanque com lote dentro), 409 `RECIPIENTE_JA_INATIVO`, 412, 428.

#### `POST /api/v1/recipientes/{recipienteId}/reativacao`

**Permissão:** `RECIPIENTE_GERENCIAR`. **Exige** `If-Match`. Sem corpo.

**Resposta:** 200 com o recurso atualizado.
**Erros:** 404, 409 `RECIPIENTE_JA_ATIVO`, 409 `NOME_RECIPIENTE_DUPLICADO` (outro recipiente ativo passou a usar o mesmo nome enquanto este estava inativo), 412, 428.

### 3.3 De onde vem a ocupação

A ocupação nasce no Diário de adega: o lote passa a ocupar um tanque com a **primeira entrada de mosto** e muda de tanque a cada **transferência**. É o módulo Adega que mantém a materialização de ocupação (spec, seção 5.4). Só que o módulo Recipiente precisa dessa informação em dois lugares: para exibir a ocupação na listagem e para impedir a inativação de um tanque ocupado.

O problema é a direção das dependências. A Adega já depende do Recipiente (ela precisa validar que o recipiente de destino de uma transferência existe e está ativo). Se o Recipiente também chamasse a Adega, os dois módulos dependeriam um do outro, e a verificação do Spring Modulith quebraria o build.

A solução é **inversão de dependência** (o "D" do SOLID):

1. O módulo Recipiente **declara** na sua interface pública o que precisa saber: uma porta `ConsultaOcupacaoRecipientes`, que recebe uma lista de ids de recipientes e devolve a ocupação de cada um.
2. O módulo Adega **implementa** essa porta, lendo a própria materialização de ocupação.
3. O Spring injeta a implementação da Adega no Recipiente. O Recipiente conhece apenas a sua própria interface, nunca a Adega.

As dependências continuam numa direção só (Adega → Recipiente), e a consulta é síncrona: a ocupação exibida é sempre a atual, sem atraso.

**Desempenho:** a listagem busca a ocupação de **todos os recipientes da página numa única consulta**, passando a lista de ids. Uma consulta por recipiente (o problema de N+1) não é aceita.

**Código do lote:** a materialização de ocupação guarda o código do lote junto com o id. Como o código é imutável (ver seção de Lotes), essa cópia nunca fica desatualizada, e a listagem de recipientes não precisa consultar o módulo Lote.

**Concorrência:** se alguém tentar inativar um tanque no mesmo instante em que um lote é transferido para ele, validar dentro de uma transação não basta. No nível de isolamento padrão do PostgreSQL, as duas transações poderiam ler "ativo" e "vazio" ao mesmo tempo e ambas seriam confirmadas (*write skew*). Por isso, as duas operações **travam a linha do recipiente** antes de validar:
- a entrada de mosto e a transferência leem o recipiente de destino com `FOR SHARE`;
- a inativação lê com `FOR UPDATE`.

Assim, uma operação espera a outra terminar e valida o estado já atualizado. O teste de integração com as duas operações concorrentes faz parte da história.

---

## 4. Lotes

Todos os endpoints exigem `X-Organization-Id`.

### 4.1 Código do lote

Todo lote tem um **código**, além do nome. O código é a identidade do lote para rastreabilidade (é o que vai no rótulo, na planilha, na conversa da adega) e **nunca muda**.

- **Informado pelo usuário:** na criação, o usuário pode informar o código seguindo a lógica própria da cervejaria. Regras: 1 a 30 caracteres; letras, números, `-`, `_`, `/` e `.`; sem espaços.
- **Gerado automaticamente:** se nenhum código for informado, o sistema gera `L-{ano}-{sequência}` (ex.: `L-2026-014`), com sequência por organização e ano.
- **Unicidade permanente:** o código é único na organização **para sempre**, incluindo lotes envasados e descartados. Reaproveitar código de lote quebraria a rastreabilidade.
- A geração automática pula códigos já usados manualmente. Ela usa um contador por organização e ano, travado durante a geração, para que dois lotes criados ao mesmo tempo não recebam o mesmo número.

No futuro, cada organização poderá configurar a máscara do código gerado (ex.: `{ESTILO}-{AAMM}-{SEQ}`). No MVP, o formato é fixo.

### 4.2 Recurso

```json
{
  "id": "0192f3c4-...",
  "codigo": "L-2026-014",
  "nome": "IPA da casa #14",
  "estado": "FERMENTANDO",
  "origem": "BEERXML",
  "receita": {
    "nome": "IPA da Casa",
    "estilo": { "codigo": "21A", "nome": "American IPA" }
  },
  "plano": {
    "volume": { "valor": 500, "unidade": "L" },
    "ogAlvo": { "valor": 1.062, "unidade": "SG" },
    "fgAlvo": { "valor": 1.012, "unidade": "SG" },
    "temperaturaFermentacao": { "minima": 18, "maxima": 20, "unidade": "CELSIUS" },
    "levedura": { "nome": "American Ale", "laboratorio": "Fermentis", "codigoProduto": "US-05" },
    "dryHopsPlanejados": [
      { "insumo": "Citra", "quantidade": { "valor": 1500, "unidade": "G" }, "diaPlanejado": 4 }
    ]
  },
  "instanteZero": "2026-09-28T09:15:00Z",
  "situacao": {
    "recipienteAtual": { "id": "0192f3b0-...", "nome": "FV-01" },
    "mosto": {
      "volumeTotal": { "valor": 10000, "unidade": "L" },
      "numeroBateladas": 5,
      "ogCalculada": { "valor": 1.061, "unidade": "SG" },
      "ogMedida": { "valor": 1.062, "unidade": "SG" },
      "ogEfetiva": { "valor": 1.062, "unidade": "SG", "origem": "MEDIDA" }
    },
    "ultimaDensidade": { "valor": 1.018, "unidade": "SG", "medidaEm": "2026-10-02T08:00:00Z" },
    "ultimaTemperatura": { "valor": 19.5, "unidade": "CELSIUS", "medidaEm": "2026-10-02T08:00:00Z" },
    "ultimaPressao": null,
    "atenuacaoAparente": 71.0,
    "ultimoEventoEm": "2026-10-02T08:00:00Z"
  },
  "envasadoEm": null,
  "descarte": null,
  "criadoEm": "2026-09-27T20:00:00Z"
}
```

| Campo | Regras |
|---|---|
| `codigo` | ver 4.1; imutável |
| `nome` | obrigatório, 1 a 120 caracteres; pode ser alterado enquanto o lote não estiver encerrado |
| `estado` | somente leitura; muda apenas por ações (ver 4.4) |
| `origem` | `MANUAL` ou `BEERXML`; somente leitura |
| `receita` | nome obrigatório; estilo opcional. É a chave usada para agrupar lotes na comparação |
| `plano` | **imutável após a criação**. Grandezas em qualquer unidade do catálogo (1.11); o banco guarda também os valores canônicos |
| `instanteZero` | somente leitura; preenchido pela inoculação registrada no Diário de adega |
| `situacao` | somente leitura; materialização mantida a cada evento registrado (spec, seção 5.4). Valores exibidos na unidade preferida do usuário são convertidos no cliente |
| `situacao.mosto` | `ogCalculada` é a média das OGs das bateladas ponderada pelo volume (em pontos de densidade); `ogMedida` é a última medição de densidade antes da inoculação, com o tanque cheio; `ogEfetiva` usa a medida quando existir e, senão, a calculada. A atenuação aparente usa a `ogEfetiva` |
| `envasadoEm` | preenchido pela ação de envase |
| `descarte` | `{ "descartadoEm": "...", "motivo": "..." }` quando descartado |

**Plano imutável na prática:** se o plano foi criado errado, o caminho é descartar o lote (com motivo "criado com erro") e criar outro. Isso é raro, e manter o plano imutável é o que garante que a comparação entre planejado e realizado seja confiável.

### 4.3 Endpoints

#### `GET /api/v1/lotes`

Lista paginada, **ordenada do lote mais recente para o mais antigo** (pelo id, que é UUIDv7 e já é ordenado no tempo). Cada item traz o resumo: id, código, nome, estado, receita e situação, sem o plano completo.

**Permissão:** `LOTE_LER`.

| Parâmetro | Uso |
|---|---|
| `estado` | um ou mais estados (ex.: `?estado=FERMENTANDO&estado=MATURANDO`) |
| `recipienteId` | lotes atualmente nesse recipiente |
| `receita` | nome da receita (comparação exata, sem diferenciar maiúsculas) |
| `codigo` | busca por prefixo do código |
| `limite`, `cursor` | paginação |

#### `GET /api/v1/lotes/{loteId}`

Lote completo, com plano. **Permissão:** `LOTE_LER`. Resposta com `ETag`. **Erros:** 404.

#### `POST /api/v1/lotes`

Cria um lote em `PLANEJADO`, manualmente ou a partir de uma importação de BeerXML já pré-visualizada.

**Permissão:** `LOTE_GERENCIAR`.

Criação manual:

```json
{
  "codigo": "IPA-2610-A",
  "nome": "IPA da casa #14",
  "receita": { "nome": "IPA da Casa", "estilo": { "codigo": "21A", "nome": "American IPA" } },
  "plano": { "...": "mesma estrutura do recurso" }
}
```

A partir de BeerXML:

```json
{
  "codigo": null,
  "nome": "IPA da casa #14",
  "importacaoId": "0192f3d0-...",
  "receitaIndice": 0
}
```

Neste caso, receita e plano vêm da importação, e o XML original é guardado junto ao lote para proveniência.

**Resposta:** 201 com o recurso e `Location`.
**Erros:** 400 (validação), 409 `CODIGO_LOTE_DUPLICADO`, 422 `CODIGO_LOTE_INVALIDO`, 404 `IMPORTACAO_NAO_ENCONTRADA` (inexistente ou expirada), 422 `RECEITA_INDICE_INVALIDO`.

#### `POST /api/v1/lotes/importacoes-beerxml`

Envia um arquivo BeerXML e recebe a pré-visualização do que será criado. **Não cria nenhum lote.** O arquivo fica guardado temporariamente (24 horas) para ser usado na criação.

**Permissão:** `LOTE_GERENCIAR`. Corpo `multipart/form-data` com o campo `arquivo`.

```json
{
  "importacaoId": "0192f3d0-...",
  "expiraEm": "2026-10-04T14:30:00Z",
  "receitas": [
    {
      "indice": 0,
      "receita": { "nome": "IPA da Casa", "estilo": { "codigo": "21A", "nome": "American IPA" } },
      "plano": { "...": "plano extraído" },
      "avisos": [
        { "code": "DRY_HOP_SEM_DIA", "params": { "insumo": "Citra" } }
      ]
    }
  ]
}
```

Um arquivo BeerXML pode conter várias receitas; o usuário escolhe uma pelo `indice`. Os `avisos` apontam informações ausentes ou ambíguas que o usuário deve conferir antes de confirmar.

**Erros:** 400 `BEERXML_INVALIDO`, 422 `BEERXML_SEM_RECEITA`, 413 (arquivo acima de 1 MB), 415 (tipo de arquivo diferente de XML).

**Segurança do XML:** arquivos XML enviados por usuários são um vetor clássico de ataque. O leitor de XML deve ter **DTDs e entidades externas desabilitadas**, o que impede ataques de XXE (leitura de arquivos do servidor) e de expansão de entidades (o "billion laughs", que derruba o servidor por consumo de memória). Além disso, há limite de tamanho e de profundidade. Um teste com arquivos maliciosos conhecidos faz parte da história.

#### `PATCH /api/v1/lotes/{loteId}`

Altera apenas o **nome**. Código, receita e plano são imutáveis.

**Permissão:** `LOTE_GERENCIAR`. **Exige** `If-Match`.
**Erros:** 400, 404, 412, 428, 422 `LOTE_ENCERRADO` (lote envasado ou descartado).

### 4.4 Ciclo de vida

```
PLANEJADO ──(inoculação registrada na Adega)──▶ FERMENTANDO
FERMENTANDO ──▶ MATURANDO ──▶ PRONTO_PARA_ENVASE ──▶ ENVASADO
FERMENTANDO ──────────────▶ PRONTO_PARA_ENVASE
qualquer estado não encerrado ──▶ DESCARTADO
MATURANDO / PRONTO_PARA_ENVASE ──(retorno de etapa)──▶ estado anterior percorrido
```

- A passagem para `FERMENTANDO` **não é uma ação deste módulo**: acontece quando a inoculação é registrada no Diário de adega, que também define o instante zero.
- `FERMENTANDO → PRONTO_PARA_ENVASE` direto existe porque muitos caseiros não fazem uma etapa de maturação separada.
- `ENVASADO` e `DESCARTADO` são estados **encerrados**: o lote não aceita mais alterações nem eventos posteriores à data de encerramento (regra de eventos atrasados, spec 3.4).

Cada transição é uma ação explícita. Todas exigem `If-Match` e a permissão `LOTE_MUDAR_ESTADO`.

| Ação | Transição | Corpo |
|---|---|---|
| `POST /lotes/{id}/maturacao` | `FERMENTANDO → MATURANDO` | `{ "ocorridoEm": "..." }` |
| `POST /lotes/{id}/liberacao-envase` | `FERMENTANDO` ou `MATURANDO → PRONTO_PARA_ENVASE` | `{ "ocorridoEm": "..." }` |
| `POST /lotes/{id}/envase` | `PRONTO_PARA_ENVASE → ENVASADO` | `{ "ocorridoEm": "..." }` |
| `POST /lotes/{id}/descarte` | qualquer não encerrado `→ DESCARTADO` | `{ "ocorridoEm": "...", "motivo": "..." }` |

- `ocorridoEm` é **quando a transição aconteceu de fato**, que pode ser antes do registro no sistema (ex.: envase feito ontem e registrado hoje). Ele não pode estar no futuro nem ser anterior ao instante zero. É esse momento que a regra de eventos atrasados usa.
- O motivo do descarte é obrigatório (1 a 500 caracteres).

**Resposta:** 200 com o lote atualizado e novo `ETag`.
**Erros:** 404, 409 `TRANSICAO_ESTADO_INVALIDA` (com `estadoAtual` e `estadoDestino` nos parâmetros), 412, 428, 422 `OCORRIDO_EM_INVALIDO`.

#### Retorno de etapa

Situações reais de adega pedem voltar atrás: o lote foi liberado para envase, mas a cerveja ainda precisa de mais maturação; ou foi marcado como maturando cedo demais. Para isso existe uma ação de retorno:

| Ação | Efeito | Corpo |
|---|---|---|
| `POST /lotes/{id}/retorno-etapa` | volta o lote ao **estado imediatamente anterior** do seu histórico | `{ "ocorridoEm": "...", "motivo": "..." }` |

Regras:

- O destino é sempre o estado anterior **efetivamente percorrido** por aquele lote. Um lote que foi de `FERMENTANDO` direto para `PRONTO_PARA_ENVASE` volta para `FERMENTANDO`, não para `MATURANDO`.
- É possível voltar a partir de `MATURANDO` e de `PRONTO_PARA_ENVASE`. Retornos sucessivos são permitidos, uma etapa por vez.
- **Não há retorno de `ENVASADO` nem de `DESCARTADO`.** Envasar é um ato físico que não se desfaz, e o descarte encerra o lote.
- **Não há retorno de `FERMENTANDO` para `PLANEJADO`.** Essa transição foi causada por uma inoculação registrada; se a inoculação foi registrada por engano, a correção é retificar o evento no Diário de adega (fora do MVP: a retificação de uma inoculação exige recalcular o instante zero e todas as posições relativas).
- O motivo é obrigatório (1 a 500 caracteres) e o retorno fica registrado no histórico de estados e na trilha de auditoria.

**Permissão:** `LOTE_MUDAR_ESTADO`. **Exige** `If-Match`.
**Erros:** 404, 409 `RETORNO_NAO_PERMITIDO` (com `estadoAtual`), 412, 428, 422 `OCORRIDO_EM_INVALIDO`.

#### `GET /api/v1/lotes/{loteId}/historico-estados`

Todas as transições do lote, inclusive retornos, ordenadas da mais recente para a mais antiga e paginadas.

**Permissão:** `LOTE_LER`.

```json
{
  "itens": [
    {
      "estadoOrigem": "PRONTO_PARA_ENVASE",
      "estadoDestino": "MATURANDO",
      "tipo": "RETORNO",
      "ocorridoEm": "2026-10-05T10:00:00Z",
      "registradoEm": "2026-10-05T10:02:13Z",
      "registradoPor": { "id": "0192f3a1-...", "nome": "Jorge Mombach" },
      "motivo": "Diacetil ainda perceptível na análise sensorial"
    }
  ],
  "proximoCursor": null
}
```

O histórico também é a fonte da regra "voltar ao estado efetivamente percorrido".

### 4.5 Como a Adega atualiza o lote

A spec (seção 4.2) previa que o Lote **escutasse** um evento de inoculação publicado pela Adega. Ao desenhar a API, apareceu o mesmo ciclo da seção 3.3: a Adega já depende do Lote (precisa perguntar se o lote aceita eventos), e escutar um evento da Adega faria o Lote depender da Adega.

A correção é a Adega **chamar a interface pública do Lote** dentro da mesma transação em que grava o evento:

- `registrarInoculacao(loteId, instante)`: muda o estado para `FERMENTANDO` e define o instante zero.
- `atualizarSituacao(loteId, ...)`: atualiza a materialização de situação a cada evento.

Isso tem duas vantagens: a dependência fica numa direção só (Adega → Lote), e a situação do lote é atualizada **na mesma transação** do evento, como a spec pede para as materializações. Os eventos de domínio publicados pela Adega continuam existindo, mas para consumidores que **só escutam**, como o futuro serviço de Alertas.

---

## 5. Diário de adega

O registro de tudo o que acontece com o lote, do mosto resfriado ao pré-envase. Todos os endpoints exigem `X-Organization-Id` e ficam aninhados ao lote, porque um evento não existe fora dele.

### 5.1 Estrutura comum dos eventos

Todo evento tem o mesmo envelope. O conteúdo específico de cada tipo fica em `dados`.

```json
{
  "id": "0192f3e1-7a2b-7c3d-9e4f-5a6b7c8d9e0f",
  "tipo": "MEDICAO",
  "ocorridoEm": "2026-10-02T08:00:00Z",
  "recebidoEm": "2026-10-02T08:00:03Z",
  "registradoPor": { "id": "0192f3a1-...", "nome": "Jorge Mombach" },
  "retificaEventoId": null,
  "retificadoPorEventoId": null,
  "motivoRetificacao": null,
  "sinalizacoes": [],
  "dados": { "metrica": "DENSIDADE", "valor": 1.018, "unidade": "SG", "valorCanonico": 1.018 }
}
```

| Campo | Quem define | Regras |
|---|---|---|
| `id` | **cliente** | UUIDv7 gerado pelo aparelho ou navegador. Obrigatório. É a chave de idempotência |
| `tipo` | cliente | um dos tipos da seção 5.2 |
| `ocorridoEm` | cliente | quando o fato aconteceu de verdade |
| `recebidoEm` | servidor | quando o servidor recebeu |
| `registradoPor` | servidor | sempre o usuário do token, nunca um valor enviado pelo cliente |
| `retificaEventoId` | servidor | preenchido quando este evento corrige outro |
| `retificadoPorEventoId` | servidor | preenchido quando este evento foi corrigido por outro |
| `sinalizacoes` | servidor | alertas para revisão humana (ver 5.5) |
| `dados` | cliente | conteúdo tipado; campos calculados pelo servidor aparecem só na resposta |

**Por que o id vem do cliente, inclusive na web:** é o mesmo mecanismo do celular offline. Usar uma regra única para todos os clientes evita dois caminhos de código, e a idempotência funciona igual em todo lugar.

### 5.2 Tipos de evento

**`MEDICAO`**

```json
{ "metrica": "DENSIDADE", "valor": 12.0, "unidade": "PLATO" }
```

| Métrica | Unidades aceitas | Canônica | Faixa aceita (canônica) |
|---|---|---|---|
| `DENSIDADE` | catálogo de densidade (1.11) | SG | 0,980 a 1,200 |
| `TEMPERATURA` | catálogo de temperatura (1.11) | °C | −15 a 110 (o limite inferior cobre o congelamento de uma eisbock) |
| `PH` | `PH` | pH | 2,0 a 8,0 |
| `PRESSAO` | catálogo de pressão (1.11) | bar | 0 a 6 |

Na resposta, `dados` inclui `valorCanonico`. Valores fora da faixa são rejeitados, porque quase sempre indicam erro de digitação (ex.: 1052 em vez de 1,052).

**`ENTRADA_MOSTO`**

```json
{
  "recipienteId": "0192f3b0-...",
  "volume": { "valor": 2000, "unidade": "L" },
  "ogCozinha": { "valor": 15.2, "unidade": "BRIX" },
  "identificacaoBrassagem": "BR-2026-0412"
}
```

Uma batelada de mosto que entrou no tanque. Um lote pode ter várias (ex.: cozinha de 2.000 L enchendo um tanque de 10.000 L em cinco bateladas); no caseiro, normalmente há uma só.

- A **primeira** entrada coloca o lote no recipiente informado. As seguintes precisam informar o mesmo recipiente em que o lote já está.
- `ogCozinha` é opcional: a OG que saiu da cozinha naquela batelada, útil para acompanhar a eficiência da brassagem e para calcular a OG do lote quando não houver medição no tanque cheio.
- `identificacaoBrassagem` é opcional: o código da batelada no controle da cozinha, de 1 a 60 caracteres.

**`OXIGENACAO`**

```json
{
  "metodo": "O2_PURO",
  "oxigenioDissolvido": { "valor": 9, "unidade": "PPM" },
  "duracaoMinutos": 20
}
```

Métodos: `O2_PURO`, `AR`, `AGITACAO`, `OUTRO`. Oxigênio dissolvido e duração são opcionais. Aceita antes e depois da inoculação (cervejas de alta densidade às vezes são reoxigenadas nas primeiras horas).

**`INOCULACAO`**

```json
{
  "levedura": { "nome": "American Ale", "laboratorio": "Fermentis", "codigoProduto": "US-05" },
  "quantidade": { "valor": 500, "unidade": "G" }
}
```

Define o instante zero do lote. Acontece no recipiente em que o lote já está, depois de todas as entradas de mosto. Unidades de quantidade: as de massa e volume do catálogo, mais `PACOTE`.

**`ADICAO`**

```json
{
  "categoria": "LUPULO",
  "insumo": "Citra",
  "quantidade": { "valor": 1500, "unidade": "G" },
  "observacao": "Primeira carga de dry hop"
}
```

Categorias: `LUPULO`, `FRUTA_ADJUNTO`, `CLARIFICANTE`, `NUTRIENTE`, `OUTRO`. O insumo é texto livre de 1 a 120 caracteres no MVP.

**`PURGA_LEVEDURA`**

```json
{ "volume": { "valor": 2, "unidade": "L" } }
```

**`COLETA_LEVEDURA`**

```json
{ "volume": { "valor": 8, "unidade": "L" }, "destino": "Reaproveitar na próxima IPA" }
```

**`SETPOINT_TEMPERATURA`**

```json
{ "temperatura": { "valor": 2, "unidade": "CELSIUS" }, "finalidade": "COLD_CRASH" }
```

Finalidades: `RAMPA`, `COLD_CRASH`, `OUTRO`. A finalidade é opcional, mas permite destacar o cold crash na linha do tempo e na comparação.

**`FILTRACAO`**

```json
{ "metodo": "PLACAS", "observacao": null }
```

Métodos: `BAG`, `PLACAS`, `CENTRIFUGA`, `OUTRO`.

**`TRANSFERENCIA`**

```json
{ "recipienteOrigemId": "0192f3b0-...", "recipienteDestinoId": "0192f3b5-..." }
```

O cliente informa a origem **que ele acredita** ser a atual. Se o lote já estiver em outro recipiente (por exemplo, alguém transferiu enquanto o celular estava offline), o evento é rejeitado em vez de criar uma ocupação inconsistente.

**`CARBONATACAO`**

```json
{
  "volumesCo2Alvo": 2.5,
  "pressao": { "valor": 1.0, "unidade": "BAR" },
  "temperatura": { "valor": 2, "unidade": "CELSIUS" }
}
```

Na resposta, `dados` inclui `volumesCo2Calculados`, obtido a partir da pressão e da temperatura.

**`ANOTACAO`**

```json
{
  "texto": "Leve amanteigado no fim de boca",
  "tags": ["SENSORIAL"],
  "offFlavors": ["DIACETIL"]
}
```

Texto de 1 a 2.000 caracteres. `offFlavors` só pode ser preenchido com a tag `SENSORIAL`. Lista proposta para o MVP:

`ACETALDEIDO`, `ACIDO_ACETICO`, `ACIDO_BUTIRICO`, `ACIDO_ISOVALERICO`, `ALCOOLICO`, `ASTRINGENTE`, `AUTOLISE`, `CLOROFENOL`, `CONTAMINACAO`, `DIACETIL`, `DMS`, `ESTERES_EXCESSIVOS`, `FENOLICO`, `GRAMINEO`, `METALICO`, `MOFO`, `OXIDADO`, `SOLVENTE`, `SULFUROSO`, `VEGETAL`.

- `CONTAMINACAO` é genérico de propósito: indica contaminação percebida sem identificação do micro-organismo, já que análise microbiológica raramente é feita em microcervejaria.
- `DIACETIL` e `DMS` são, de longe, os mais comuns nesta etapa; a interface os mostra primeiro.
- `LIGHTSTRUCK` ficou de fora porque aparece depois do envase, fora do escopo do diário de adega. A lista da cerveja pronta pertence ao futuro módulo sensorial.

### 5.3 Regras de registro

| Regra | Erro |
|---|---|
| O lote precisa existir na organização | 404 |
| O lote não pode estar `DESCARTADO` ou `ENVASADO` com `ocorridoEm` **posterior** ao encerramento. Eventos anteriores ao encerramento são aceitos (spec, seção 3.4) | 422 `LOTE_NAO_ACEITA_EVENTOS` |
| Só existe uma inoculação por lote | 409 `INOCULACAO_JA_REGISTRADA` |
| Antes da inoculação, só são aceitos `ENTRADA_MOSTO`, `OXIGENACAO`, `MEDICAO` (ex.: OG com o tanque cheio, pH) e `ANOTACAO` | 422 `LOTE_NAO_INOCULADO` |
| A inoculação exige que o lote já esteja num recipiente (ao menos uma entrada de mosto) | 422 `LOTE_SEM_MOSTO` |
| Depois da inoculação, não são aceitas novas entradas de mosto | 422 `LOTE_JA_INOCULADO` |
| Entradas de mosto seguintes à primeira precisam informar o recipiente atual do lote | 409 `RECIPIENTE_ORIGEM_DIVERGENTE` |
| Recipiente da primeira entrada de mosto ou de destino de transferência precisa estar ativo e vazio. Dois lotes no mesmo tanque (blend) ficam fora do MVP | 422 `RECIPIENTE_INATIVO`, 409 `RECIPIENTE_OCUPADO` |
| Na transferência, a origem informada precisa ser o recipiente atual do lote, e o destino precisa ser diferente da origem | 409 `RECIPIENTE_ORIGEM_DIVERGENTE`, 422 `TRANSFERENCIA_MESMO_RECIPIENTE` |
| Unidade compatível com a métrica e valor dentro da faixa | 422 `UNIDADE_INVALIDA`, 422 `VALOR_FORA_DA_FAIXA` |
| Mesmo `id` com conteúdo idêntico | **não é erro**: devolve o evento existente (ver 5.4) |
| Mesmo `id` com conteúdo diferente | 409 `ID_EVENTO_CONFLITANTE`, registrado em log como erro de cliente |

### 5.4 Endpoints

#### `POST /api/v1/lotes/{loteId}/eventos`

Registra um evento. Usado pela web e pelo desktop. O celular usa o endpoint de sincronização (seção 6), que aplica exatamente as mesmas regras.

**Permissão:** `EVENTO_REGISTRAR`.

**Resposta:**
- **201** com o evento, quando é novo;
- **200** com o evento existente, quando o mesmo `id` com o mesmo conteúdo já tinha sido registrado. Para o cliente, as duas respostas significam sucesso.

O registro, a atualização da situação do lote, a atualização da ocupação (na primeira entrada de mosto e nas transferências) e a publicação do evento de domínio acontecem **na mesma transação**.

#### `GET /api/v1/lotes/{loteId}/eventos`

A linha do tempo do lote, paginada, **do evento mais recente para o mais antigo** (por `ocorridoEm` e, no empate, por `id`).

**Permissão:** `EVENTO_LER`.

| Parâmetro | Uso |
|---|---|
| `tipo` | um ou mais tipos |
| `de`, `ate` | intervalo de `ocorridoEm` |
| `incluirRetificados` | padrão `false`: só a versão vigente de cada evento. Com `true`, inclui as versões corrigidas, para auditoria |
| `limite`, `cursor` | paginação |

Índice que sustenta a consulta: `(lote_id, ocorrido_em DESC, id DESC)`.

#### `GET /api/v1/lotes/{loteId}/eventos/{eventoId}`

Um evento específico, vigente ou retificado. **Permissão:** `EVENTO_LER`. **Erros:** 404.

#### `GET /api/v1/lotes/{loteId}/medicoes`

A série temporal de uma métrica, para os gráficos do lote. Só considera medições vigentes.

**Permissão:** `EVENTO_LER`.

| Parâmetro | Uso |
|---|---|
| `metrica` | **obrigatório** |
| `de`, `ate` | intervalo de `ocorridoEm` |
| `limite`, `cursor` | paginação; padrão 500, máximo 2.000, porque séries são lidas em blocos maiores |

Ordenada da **mais antiga para a mais recente**, que é a ordem natural de um gráfico.

```json
{
  "itens": [
    {
      "eventoId": "0192f3e1-...",
      "ocorridoEm": "2026-10-02T08:00:00Z",
      "horasDesdeInoculacao": 94.75,
      "valorCanonico": 1.018,
      "valorInformado": 4.6,
      "unidadeInformada": "PLATO"
    }
  ],
  "proximoCursor": null
}
```

`horasDesdeInoculacao` é **calculado na consulta**, não guardado (spec, seção 5.4). É `null` se o lote ainda não foi inoculado, e negativo para medições anteriores à inoculação.

Índice que sustenta a consulta: `(lote_id, metrica, ocorrido_em)`.

### 5.5 Sinalizações

Situações suspeitas não são rejeitadas, porque o dado pode ser verdadeiro. Elas são aceitas e marcadas para revisão:

| Sinalização | Quando |
|---|---|
| `RELOGIO_ADIANTADO` | `ocorridoEm` mais de 5 minutos à frente de `recebidoEm` |
| `REGISTRO_MUITO_TARDIO` | `recebidoEm` mais de 7 dias depois de `ocorridoEm` |

Os limites são configuráveis. A interface mostra um aviso nos eventos sinalizados.

### 5.6 Retificação

Eventos nunca são editados nem apagados. Uma correção cria um **novo evento**, que aponta para o original e passa a ser a versão vigente.

#### `POST /api/v1/lotes/{loteId}/eventos/{eventoId}/retificacoes`

**Permissão:** `EVENTO_RETIFICAR`.

```json
{
  "id": "0192f3f0-...",
  "ocorridoEm": "2026-10-02T08:00:00Z",
  "dados": { "metrica": "DENSIDADE", "valor": 1.025, "unidade": "SG" },
  "motivo": "Digitei 1,052 em vez de 1,025"
}
```

Ou, para anular um evento que não deveria existir:

```json
{ "id": "0192f3f1-...", "anular": true, "motivo": "Registrado no lote errado" }
```

**Regras:**

- O `id` da retificação também é gerado pelo cliente e também é idempotente.
- A retificação tem o **mesmo tipo** do evento original; para mudar o tipo, anula-se o original e registra-se um novo.
- Só a **versão vigente** pode ser retificada. Corrigir uma versão antiga devolve 409 `EVENTO_JA_RETIFICADO`, com o id da versão vigente nos parâmetros.
- O motivo é obrigatório (1 a 500 caracteres).
- As regras de registro (5.3) valem também para o conteúdo corrigido.

**Restrições para os eventos que afetam a ocupação dos tanques ou o instante zero:**

| Tipo | Pode corrigir | Não pode no MVP |
|---|---|---|
| `ENTRADA_MOSTO` | `ocorridoEm`, volume, OG da cozinha e identificação da brassagem | trocar o recipiente; anular |
| `INOCULACAO` | `ocorridoEm`, levedura e quantidade | anular |
| `TRANSFERENCIA` | `ocorridoEm` | trocar origem ou destino; anular |

Corrigir volume ou OG de uma entrada de mosto recalcula a OG do lote na mesma transação.

**Correção do momento da inoculação:** atualiza também o instante zero do lote, na mesma transação. Como o tempo relativo das medições é calculado na consulta, nenhuma medição precisa ser recalculada. O novo instante zero não pode ser posterior a nenhuma transição de estado do lote (erro 422 `INSTANTE_ZERO_INVALIDO`).

**Resposta:** 201 com o novo evento vigente.
**Erros:** 404, 409 `EVENTO_JA_RETIFICADO`, 422 `RETIFICACAO_NAO_PERMITIDA` (com o campo ou a operação bloqueada), 422 `LOTE_NAO_ACEITA_EVENTOS`, mais os erros de 5.3.

### 5.7 Quadro de tanques

Não existe endpoint dedicado. O quadro é montado com duas consultas que já existem:

- `GET /recipientes`, que traz a ocupação de cada tanque (inclusive os vazios);
- `GET /lotes?estado=FERMENTANDO&estado=MATURANDO&estado=PRONTO_PARA_ENVASE`, que traz a situação de cada lote ativo (últimas leituras e atenuação).

Um endpoint de composição só será criado se essas duas chamadas se mostrarem insuficientes na prática.

---

## 6. Sincronização (mobile)

O app mobile registra eventos numa fila local e os envia quando há rede (spec, seção 6). Esta seção define o endpoint que recebe essa fila. **As regras de negócio são exatamente as da seção 5**: a sincronização é apenas outra porta de entrada para os mesmos casos de uso, nunca uma versão paralela das regras.

### 6.1 Endpoint

#### `POST /api/v1/sync/eventos`

Recebe uma sequência de registros e retificações, de um ou mais lotes da mesma organização, e devolve um resultado para cada item.

**Exige** `X-Organization-Id`. A permissão (`EVENTO_REGISTRAR` ou `EVENTO_RETIFICAR`) é verificada **item a item**.

```json
{
  "itens": [
    {
      "operacao": "REGISTRAR",
      "loteId": "0192f3c4-...",
      "evento": {
        "id": "0192f401-...",
        "tipo": "MEDICAO",
        "ocorridoEm": "2026-10-02T08:00:00Z",
        "dados": { "metrica": "DENSIDADE", "valor": 4.6, "unidade": "BRIX" }
      }
    },
    {
      "operacao": "RETIFICAR",
      "loteId": "0192f3c4-...",
      "eventoRetificadoId": "0192f401-...",
      "retificacao": {
        "id": "0192f402-...",
        "ocorridoEm": "2026-10-02T08:00:00Z",
        "dados": { "metrica": "DENSIDADE", "valor": 4.8, "unidade": "BRIX" },
        "motivo": "Leitura refeita com refratômetro calibrado"
      }
    }
  ]
}
```

- `evento` tem a mesma estrutura do `POST /lotes/{loteId}/eventos`, e `retificacao`, a do `POST .../retificacoes` (seção 5).
- **Máximo de 100 itens por requisição.** Uma fila maior é enviada em várias requisições, na ordem. Acima do limite: 413.
- Os itens são processados **na ordem enviada**, que é a ordem de criação no aparelho. Isso permite que uma retificação aponte para um evento do mesmo envio.

**Resposta:** sempre **200** quando a requisição em si é válida, mesmo que todos os itens tenham sido rejeitados. O resultado de cada item vem na lista, na mesma ordem do envio.

```json
{
  "resultados": [
    { "id": "0192f401-...", "status": "ACEITO", "evento": { "...": "evento como gravado" } },
    { "id": "0192f402-...", "status": "REJEITADO", "code": "LOTE_NAO_ACEITA_EVENTOS", "params": { "loteId": "0192f3c4-...", "encerradoEm": "2026-10-01T18:00:00Z" } }
  ],
  "processadoEm": "2026-10-02T11:40:12Z"
}
```

Erros da requisição inteira seguem as convenções gerais: 400 (estrutura inválida), 401, 403 (sem vínculo com a organização), 413, 429.

### 6.2 Status por item

| Status | Significado | O que o app faz |
|---|---|---|
| `ACEITO` | Gravado agora | remove da fila e atualiza a tela com o evento devolvido |
| `DUPLICADO` | Já tinha sido gravado antes, com o mesmo conteúdo (ex.: a resposta anterior se perdeu na rede) | **igual ao aceito**: remove da fila |
| `REJEITADO` | Violou uma regra de negócio; reenviar não vai mudar o resultado | move para a **caixa de conflitos**, com a mensagem traduzida do `code` |
| `ADIADO` | Falha temporária ao processar aquele item (ex.: o lote estava travado por outra operação) | mantém na fila e tenta de novo no próximo ciclo |

A distinção entre `REJEITADO` e `ADIADO` é o que impede dois erros opostos: reenviar para sempre algo que nunca vai passar, e descartar algo que passaria na próxima tentativa.

**Dependência dentro do envio:** se uma retificação aponta para um evento do mesmo envio que foi rejeitado ou adiado, ela recebe o mesmo destino, com o `code` `DEPENDENCIA_NAO_PROCESSADA`.

### 6.3 Transações

**Cada item é processado na sua própria transação.** Um item rejeitado não desfaz os aceitos antes dele, e uma falha no meio do envio não perde o que já foi gravado. Como cada item é idempotente pelo `id`, reenviar o envio inteiro depois de uma falha de rede é sempre seguro: os itens já gravados voltam como `DUPLICADO`.

### 6.4 Comportamento do app

**Ciclo de sincronização:** dispara quando a conexão volta, quando o app é aberto e quando o usuário toca em "sincronizar". Envia a fila em blocos de até 100 itens, em ordem, e só passa para o próximo bloco depois de processar a resposta do anterior.

**Falhas da requisição inteira:**

| Situação | O que o app faz |
|---|---|
| Sem rede, tempo esgotado, erro 5xx | mantém tudo na fila e tenta de novo com espera crescente (*backoff* exponencial com variação aleatória) |
| 429 | respeita o `Retry-After` |
| 401 | tenta renovar a sessão; se não conseguir, pede login. **A fila nunca é apagada** |
| 403 | o usuário perdeu acesso à organização; a fila é mantida e o app avisa, sem descartar nada sozinho |

**Caixa de conflitos:** cada item rejeitado mostra o evento, o motivo traduzido e duas ações:
- **Descartar:** remove o item do aparelho. Nada é enviado ao servidor.
- **Reatribuir a outro lote:** cria um **novo** registro, com novo `id`, para o lote escolhido, e o envia na próxima sincronização. O item rejeitado é removido. Usar um novo `id` evita qualquer ambiguidade com o registro original.

**Várias organizações:** cada item da fila guarda a organização em que foi criado. A sincronização agrupa os itens por organização e envia cada grupo com o `X-Organization-Id` correspondente.

### 6.5 Cache de leitura

O app guarda localmente o necessário para registrar eventos sem rede: os recipientes e os lotes em andamento, com suas situações. **Não há endpoint específico para isso**: o cache é preenchido pelas consultas que já existem (`GET /recipientes` e `GET /lotes` filtrado pelos estados em andamento). A hora da última atualização é exibida na tela ("atualizado há 12 min").

A validação local (faixas de valor, unidades, regras simples de estado) usa o pacote compartilhado e serve apenas para dar retorno imediato ao operador. **A decisão final é sempre do servidor.**

### 6.6 Limites

- Limite de requisições do `/sync/eventos` mais restrito que o geral (ex.: 30 por minuto por usuário), ajustável por configuração.
- Até 100 itens por requisição.
- Um teste de carga simples (ex.: 1.000 eventos em fila, enviados em blocos) faz parte da história, para medir o tempo de sincronização depois de um dia inteiro sem rede.

---

## 7. Comparação de lotes

Sobrepor as curvas de vários lotes no mesmo gráfico, alinhadas pelo tempo desde a inoculação, com as intervenções marcadas. É o recurso que responde "por que este lote ficou diferente do anterior?".

### 7.1 Escolha dos lotes

Não há endpoint específico para escolher os lotes. A tela usa `GET /lotes` com os filtros que já existem (ex.: `?receita=IPA da Casa`) e o usuário marca os que quer comparar.

### 7.2 Endpoint

#### `GET /api/v1/comparacoes`

**Permissão:** `COMPARACAO_LER`. **Exige** `X-Organization-Id`.

| Parâmetro | Uso |
|---|---|
| `loteId` | repetido, **de 2 a 6 lotes** (ex.: `?loteId=...&loteId=...`). Mais que isso deixa o gráfico ilegível e a consulta cara |
| `metrica` | repetido; padrão `DENSIDADE` e `TEMPERATURA` |
| `deHoras`, `ateHoras` | janela em horas relativas à inoculação; padrão de −24 h até o último ponto do lote mais longo |
| `marcos` | tipos de evento a marcar na linha do tempo; padrão: todos, exceto medições |

É um `GET` porque a consulta não altera nada e pode ser repetida ou guardada como link.

```json
{
  "eixo": "HORAS_DESDE_INOCULACAO",
  "resolucaoAplicada": null,
  "lotes": [
    {
      "id": "0192f3c4-...",
      "codigo": "L-2026-014",
      "nome": "IPA da casa #14",
      "estado": "MATURANDO",
      "instanteZero": "2026-09-28T09:15:00Z",
      "referencias": {
        "ogAlvo": 1.062,
        "fgAlvo": 1.012,
        "ogEfetiva": 1.062,
        "temperaturaFermentacao": { "minima": 18, "maxima": 20 }
      },
      "series": {
        "DENSIDADE": [
          { "horas": -2.5, "valor": 1.062, "pontosAgregados": 1 },
          { "horas": 22.0, "valor": 1.048, "pontosAgregados": 1 }
        ],
        "TEMPERATURA": [
          { "horas": 0.0, "valor": 19.0, "pontosAgregados": 1 }
        ]
      },
      "marcos": [
        {
          "horas": 96.0,
          "eventoId": "0192f4a0-...",
          "tipo": "ADICAO",
          "resumo": { "categoria": "LUPULO", "insumo": "Citra", "quantidade": { "valor": 1500, "unidade": "G" } }
        },
        {
          "horas": 240.0,
          "eventoId": "0192f4b2-...",
          "tipo": "SETPOINT_TEMPERATURA",
          "resumo": { "temperatura": { "valor": 2, "unidade": "CELSIUS" }, "finalidade": "COLD_CRASH" }
        }
      ]
    }
  ],
  "avisos": [
    { "code": "LOTE_NAO_INOCULADO", "params": { "loteId": "0192f3d9-..." } }
  ]
}
```

- **Todos os valores vêm na unidade canônica.** A conversão para a unidade preferida do usuário é feita no cliente, como no resto da API.
- `referencias` permite desenhar as linhas de OG/FG alvo e a faixa de temperatura planejada, comparando o planejado com o realizado. A curva de atenuação aparente pode ser calculada no cliente a partir da densidade e da `ogEfetiva`.
- Só entram eventos e medições **vigentes** (retificados ficam de fora).
- Um lote sem inoculação não tem eixo de tempo: ele aparece em `avisos` e não tem séries.
- **Erros:** 400 `QUANTIDADE_LOTES_INVALIDA`, 400 `METRICA_INVALIDA`, 404 `LOTE_NAO_ENCONTRADO` (com o id; vale também para lote de outra organização).

### 7.3 Volume de pontos e agregação

Esta resposta **não é paginada**, porque uma comparação pela metade não tem utilidade. O tamanho é limitado de outra forma:

- Com registro manual, cada série tem dezenas de pontos, e a resposta é pequena.
- Quando houver sensores (um ponto a cada poucos minutos), uma série pode ter milhares de pontos. Se alguma série passar de **1.000 pontos**, o servidor **agrega** os pontos em intervalos de tempo (média por intervalo), escolhendo o menor intervalo que respeite o limite. O intervalo usado volta em `resolucaoAplicada` (ex.: `"PT1H"`), e cada ponto informa quantas leituras representa em `pontosAgregados`.

Esta é a segunda exceção à regra de paginação (a primeira é o `GET /me`). As duas serão registradas no mesmo ADR.

### 7.4 Como a consulta funciona

O módulo Comparação **não lê tabelas de outros módulos** (spec, seção 4.2). Ele orquestra:

1. Pede ao módulo **Lote**, pela interface pública, os dados dos lotes selecionados: instante zero, referências do plano e OG efetiva, numa única chamada.
2. Pede ao módulo **Adega**, pela interface pública, as séries e os marcos desses lotes, passando os instantes zero.
3. Monta a resposta.

A consulta pesada mora no adaptador de persistência da Adega. O PostgreSQL tem uma função que encaixa bem no problema: `date_bin`, que agrupa instantes em intervalos **a partir de uma origem escolhida**. Usando o instante zero de cada lote como origem, os intervalos de todos os lotes ficam alinhados à inoculação:

```sql
WITH zero AS (
  SELECT * FROM unnest(:loteIds::uuid[], :instantesZero::timestamptz[])
         AS z(lote_id, instante_zero)
)
SELECT m.lote_id,
       m.metrica,
       date_bin(:intervalo, m.ocorrido_em, z.instante_zero) AS inicio_intervalo,
       avg(m.valor_canonico)                               AS valor,
       count(*)                                            AS pontos_agregados
FROM adega.medicao_vigente m
JOIN zero z USING (lote_id)
WHERE m.metrica = ANY(:metricas)
  AND m.ocorrido_em BETWEEN z.instante_zero + :de AND z.instante_zero + :ate
GROUP BY m.lote_id, m.metrica, inicio_intervalo
ORDER BY m.lote_id, m.metrica, inicio_intervalo;
```

- Sem agregação (o caso comum no MVP), a mesma consulta roda sem o `GROUP BY`.
- O índice `(lote_id, metrica, ocorrido_em)` da tabela de medições sustenta o filtro: a consulta lê só as faixas de tempo de cada lote, nunca a tabela inteira.
- As horas relativas (`inicio_intervalo − instante_zero`) são calculadas na consulta, como definido na spec (seção 5.4).
- A consulta real é escrita com o jOOQ; o SQL acima é a referência do que ela deve gerar, e o plano de execução dela acompanha a história.
