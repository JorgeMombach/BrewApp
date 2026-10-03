# BrewApp — Design do MVP: Diário de Adega

- **Data:** 2026-10-03
- **Status:** em revisão
- **Autor:** Jorge Mombach (com apoio do Claude)

---

## 1. Contexto e problema

O planejamento de receitas cervejeiras já é bem atendido por ferramentas consolidadas como o BeerSmith, padrão entre cervejeiros caseiros e pequenas cervejarias. Competir nesse espaço não é o objetivo.

O problema está **depois da brassagem**. O acompanhamento da fermentação e da maturação (leituras de densidade e temperatura, dry hop, purgas de levedura, clarificação, carbonatação) ainda é feito no papel e depois transcrito para planilhas, tanto em casa quanto em cervejarias. Isso gera retrabalho, perda de informação e torna quase impossível responder a pergunta que mais importa para manter um padrão: *por que este lote ficou diferente do anterior?*

O BrewApp ataca exatamente esse intervalo: **do mosto resfriado até o momento imediatamente anterior ao envase.**

### Objetivos

1. Substituir o papel por um **diário de adega estruturado**, em que cada intervenção é um registro tipado (com data, quantidade e responsável), e não uma linha de texto livre.
2. Permitir **comparar lotes lado a lado**, sobrepondo as curvas de fermentação pelo tempo relativo à inoculação, para enxergar e manter a consistência entre lotes.
3. Fazer o dado nascer **na mão de quem mede**: registro no celular, inclusive sem rede, e análise no desktop.
4. Servir como **projeto de estudo e portfólio**, com decisões de arquitetura explícitas e justificadas.

### Público

- **Agora (MVP):** uso pessoal, um único usuário real.
- **Futuro possível:** micro e médias cervejarias, eventualmente como SaaS.

O MVP **não constrói** nada exclusivo do cenário SaaS (billing, onboarding, telas de administração de organizações). Mas o modelo de dados, a autenticação e a autorização já nascem **preparados para múltiplas organizações e múltiplos usuários com papéis diferentes**, para que essa evolução não exija reescrever o núcleo.

### Critérios de sucesso do MVP

- Conseguir acompanhar um lote real do começo ao fim usando apenas o app, sem papel.
- Registrar uma leitura no celular em poucos toques, inclusive sem conexão, sem perder nenhum registro.
- Comparar visualmente dois ou mais lotes da mesma receita e identificar diferenças de comportamento e de intervenções.

---

## 2. Escopo

### Dentro do MVP

- **Organização e identidade:** organização (a cervejaria), usuário e vínculo com papel.
- **Recipientes:** cadastro de fermentadores e tanques.
- **Lotes:** criação manual ou por importação de BeerXML, plano imutável e ciclo de vida.
- **Diário de adega:** registro de eventos tipados, retificações e ocupação dos tanques.
- **Comparação:** sobreposição de curvas de lotes em tempo relativo, com os eventos marcados.
- **Internacionalização:** pt-BR, espanhol e inglês desde o início.
- **Clientes:** web (React), desktop (Tauri) e mobile (React Native), com offline no mobile para registro de eventos.

### Fora do MVP (registrado para o futuro)

| Módulo futuro | Observação |
|---|---|
| Alertas e agendamentos | Primeira extração planejada para microsserviço (ver seção 4.4) |
| Gerações de levedura | Rastreio de origem, número de gerações e viabilidade. A coleta de levedura do MVP já é o gancho para isso |
| Integração com sensores | iSpindel, Tilt, termostatos dos tanques. Segundo candidato a microsserviço |
| Inventário | Os insumos dos eventos passarão a referenciar itens de estoque |
| Análise sensorial | As tags de off-flavor do MVP já são a semente |
| Custos e relatórios | Candidatos a visões materializadas com atualização periódica |
| Blend de lotes | **Primeiro candidato pós-MVP.** Juntar lotes já fermentados num tanque (para envase ou para corrigir um lote com defeito) é prática comum em cervejarias. O lote resultante deve registrar os lotes de origem e a proporção de cada um, preservando a rastreabilidade. Não confundir com várias bateladas de mosto enchendo um tanque, que é um único lote e está no MVP |
| Receitas | Fora de foco; o BeerSmith continua sendo a fonte, via BeerXML |
| Traduções no banco | Quando houver tradutores que não são desenvolvedores (ver seção 8) |
| Padrão BFF na web | Evolução de segurança do cliente web (ver seção 9) |

---

## 3. Domínio

### 3.1 Lote

O lote é o agregado central. Ele nasce manualmente ou pela importação de um arquivo **BeerXML** exportado do BeerSmith, que traz a identidade da receita, as densidades alvo, a levedura e os lúpulos de dry hop planejados.

O **plano** do lote é um *snapshot* imutável. Se a receita mudar depois no BeerSmith, o histórico do lote continua refletindo o que foi planejado no momento em que ele foi criado. O arquivo BeerXML original fica guardado para fins de proveniência.

**Ciclo de vida:**

`PLANEJADO → FERMENTANDO → MATURANDO → PRONTO_PARA_ENVASE → ENVASADO`

O status `DESCARTADO` pode ser alcançado a partir de qualquer etapa. O envase em si é o limite do escopo: o MVP termina no momento em que o lote é marcado como envasado.

**Código do lote:** além do nome, todo lote tem um código imutável, único na organização para sempre (inclusive após envase ou descarte), que serve à rastreabilidade. O usuário pode informá-lo seguindo a lógica própria da cervejaria; se não informar, o sistema gera um código sequencial por organização e ano (ex.: `L-2026-014`).

**Marco zero:** a inoculação define o instante zero do lote. Todos os eventos são posicionados em relação a ele, o que permite comparar lotes de datas diferentes. Uma medição feita antes da inoculação (por exemplo, a densidade original do mosto resfriado) aparece em tempo negativo.

### 3.2 Recipientes

Fermentadores e tanques, com nome, tipo e capacidade. A ocupação de cada tanque ("o que está no tanque 3 agora") é **sempre derivada** dos eventos (o lote ocupa um tanque a partir da primeira entrada de mosto e muda de tanque a cada transferência), e nunca preenchida à mão.

Um lote pode passar por vários recipientes ao longo da vida (por exemplo, de fermentador para maturador). No cenário caseiro, isso se reduz naturalmente a um lote em um único recipiente, sem nenhuma transferência.

### 3.3 Catálogo de eventos

| Evento | O que registra | Observações |
|---|---|---|
| **Medição** | métrica, valor e unidade | Métricas: densidade, temperatura, pH e pressão. Uma estrutura única, para que novas métricas não exijam um novo tipo de evento. Densidade é medida diariamente, temperatura com frequência, pH esporadicamente. |
| **Entrada de mosto** | recipiente, volume, OG da cozinha (opcional) e identificação da brassagem (opcional) | Uma batelada de mosto entrando no tanque. Um lote pode ter várias (cozinha menor que o tanque). A primeira coloca o lote no tanque. Não é aceita depois da inoculação. |
| **Oxigenação** | método, oxigênio dissolvido (opcional) e duração (opcional) | Aceita antes e depois da inoculação. |
| **Inoculação** | levedura e quantidade | Define o marco zero do lote. Acontece com o tanque já cheio, no recipiente em que o lote está. |
| **Adição** | categoria, insumo, quantidade e unidade | Categorias: lúpulo (dry hop), fruta ou adjunto, clarificante, nutriente, outro agente. |
| **Purga de levedura** | volume | Pode se repetir ao longo do processo. |
| **Coleta de levedura** | volume e destino | Normalmente acontece uma vez. É separada da purga porque será o ponto de partida do módulo de gerações de levedura. |
| **Setpoint de temperatura** | temperatura alvo | Acompanha as rampas de fermentação. O cold crash é um setpoint. |
| **Filtração ou clarificação física** | método | Bag, filtro de placas, centrífuga ou outro. Clarificação por agente químico é uma Adição; por equipamento, é este evento. |
| **Transferência** | recipiente de origem e de destino | Atualiza a ocupação dos tanques. |
| **Carbonatação** | volumes de CO₂ alvo; pressão e temperatura medidas | O app calcula os volumes de CO₂ a partir da pressão e da temperatura (leitura de um testador como o Zahm & Nagel). |
| **Anotação** | texto livre e tags opcionais | A tag "sensorial" pode vir acompanhada de off-flavors de uma lista fixa (diacetil, DMS, contaminação, acetaldeído, oxidação, entre outros). |

### 3.4 Regras de domínio

- **Cada tipo de evento tem um conteúdo próprio, tipado e validado.** A generalização acontece apenas *dentro* de um tipo (como a categoria da Adição). Isso evita o anti-padrão de uma tabela genérica de "chave e valor", em que nada é validado.
- **Eventos são imutáveis.** Um erro de digitação é corrigido por um evento de **retificação**, que aponta para o evento original. As curvas usam o valor corrigido, e a auditoria mostra os dois.
- **Unidades:** cada medição guarda o valor **como foi informado** (por exemplo, 12 °P) e também o valor **canônico** (gravidade específica para densidade, graus Celsius para temperatura). A comparação usa o valor canônico; a tela mostra o valor informado.
- **Insumos são texto livre no MVP,** mas com nome, quantidade e unidade em campos separados, prontos para referenciar o inventário no futuro.
- **Momento da medição e momento do recebimento são distintos.** O momento da medição vem do aparelho de quem mediu; o momento do recebimento é registrado pelo servidor. Diferenças absurdas (como uma medição "no futuro") são sinalizadas para revisão, e não rejeitadas.
- **Eventos atrasados em lotes que mudaram de estado:** se um evento chega depois de o lote ter sido envasado, ele é **aceito se aconteceu antes do envase** (é um fato real que apenas chegou atrasado) e **rejeitado se aconteceu depois** (provavelmente foi registrado no lote errado). Eventos rejeitados vão para uma caixa de conflitos, onde o usuário decide se descarta ou reatribui o evento a outro lote.

### 3.5 Diário de eventos não é Event Sourcing

Os eventos de adega são **dados de negócio** que só recebem inclusões. O status do lote e os cadastros continuam sendo estado comum no banco. O sistema **não** reconstrói seu estado a partir dos eventos, como faria uma arquitetura de Event Sourcing completa. Essa escolha mantém a simplicidade e merece um registro de decisão próprio.

---

## 4. Arquitetura

### 4.1 Visão geral

| Camada | Tecnologia |
|---|---|
| Backend | Java 25 (LTS), Spring Boot 4, Spring Modulith, Spring Security |
| Banco de dados | PostgreSQL 18, Flyway, jOOQ |
| Build do backend | Maven |
| Web e desktop | React, TypeScript, Vite, Tauri |
| Mobile | React Native (Expo) |
| Autenticação | Keycloak (OpenID Connect) |
| Mensageria | RabbitMQ, a partir da primeira extração de serviço |
| Logs | SLF4J, com Log4j2 como implementação |

O projeto é um **monorepo**, com o backend, a aplicação web/desktop, o aplicativo mobile, um pacote TypeScript compartilhado (tipos da API gerados automaticamente e regras de conversão de unidades), a infraestrutura local (docker-compose) e a documentação, incluindo os registros de decisão (ADRs).

### 4.2 Monolito modular

O backend começa como **um único serviço, dividido em módulos com fronteiras rígidas**, verificadas automaticamente pelo Spring Modulith a cada build. Microsserviços desde o primeiro dia trariam custos (consistência eventual, transações distribuídas, várias pipelines e bancos) antes de o produto entregar seu primeiro valor. O monolito modular entrega rápido e mantém aberta a porta para extrair serviços quando houver motivo real.

| Módulo | Responsabilidade |
|---|---|
| Organização | organizações, usuários, vínculos e papéis |
| Recipiente | cadastro de tanques e fermentadores |
| Lote | lotes, plano, ciclo de vida e importação de BeerXML |
| Adega | registro de eventos, retificações e ocupação de tanques |
| Comparação | sobreposição de curvas; apenas leitura |
| Núcleo compartilhado | tipos de valor como quantidade, unidade e identificadores; o mínimo possível |

**Comunicação entre módulos:**

- **As dependências entre módulos têm uma direção só.** A Adega depende do Lote e do Recipiente; nenhum dos dois depende da Adega. Dependência circular quebra o build (verificação do Spring Modulith).
- **Chamadas síncronas** pela interface pública do módulo, quando a operação precisa de resposta ou precisa acontecer na mesma transação. Exemplos: a Adega pergunta ao Lote se ele ainda aceita eventos; ao registrar uma inoculação, a Adega chama o Lote para mudar o estado para fermentando e definir o instante zero; a cada evento, a Adega chama o Lote para atualizar a situação materializada. Tudo na mesma transação em que o evento é gravado.
- **Inversão de dependência** quando um módulo precisa de uma informação que pertence a outro que depende dele. Exemplo: o Recipiente precisa saber a ocupação dos tanques, que pertence à Adega. O Recipiente declara uma interface ("me diga a ocupação destes tanques") e a Adega a implementa, mantendo a direção Adega → Recipiente.
- **Eventos de domínio** são publicados para consumidores que apenas reagem a fatos, sem que o publicador dependa deles. Exemplo: a Adega publica "inoculação registrada" e "dry hop registrado", e o futuro serviço de Alertas os consome.

### 4.3 Organização interna de cada módulo

Cada módulo segue a arquitetura hexagonal (portas e adaptadores):

- **Domínio:** Java puro, sem dependência de framework ou de banco.
- **Aplicação:** casos de uso e as interfaces (portas) de que eles precisam, como os repositórios.
- **Adaptadores:** de entrada (controllers REST) e de saída (persistência com jOOQ).

Conversões entre representações acontecem **apenas nas fronteiras** (REST para aplicação, domínio para persistência). O uso de `record` mantém essas conversões enxutas, evitando a proliferação de DTOs e mappers sem propósito.

### 4.4 Primeira extração: Alertas

Logo após o fluxo central funcionar, o módulo de **Alertas e agendamentos** ("dry hop no dia 4", "leitura atrasada há 24 horas", "cold crash amanhã") será o primeiro extraído como microsserviço. Ele é assíncrono por natureza, tem ciclo de vida próprio e só precisa consumir os eventos dos lotes.

O caminho já está preparado: o Spring Modulith grava os eventos de domínio na mesma transação do caso de uso (padrão *outbox*), garantindo que nenhum se perca. Na extração, esses eventos passam a ser publicados no RabbitMQ, quase sem mudança no domínio. Infraestrutura em nuvem (como ECS e, eventualmente, Kubernetes) só entra quando a escala justificar.

### 4.5 Contrato da API

A API é REST, com o contrato OpenAPI gerado a partir do código do backend. O pacote compartilhado gera os tipos TypeScript a partir desse contrato, e o CI falha se os tipos estiverem desatualizados.

As regras de cálculo que precisam rodar nos dois lados (conversão entre graus Plato e gravidade específica, volumes de CO₂) são implementadas em Java (fonte da verdade) e em TypeScript (pré-visualização no aparelho, inclusive sem rede). Para garantir que não divirjam, **as duas implementações são testadas contra o mesmo arquivo de casos de teste.**

---

## 5. Dados e persistência

### 5.1 Princípios

- **Um schema do PostgreSQL por módulo.** Nenhum módulo lê as tabelas de outro. É isso que torna a extração de serviços viável.
- **O banco é a fonte da verdade do schema.** O Flyway aplica as migrations, e o jOOQ gera as classes a partir do banco real (um PostgreSQL em container durante o build). O código gerado não é versionado.
- **Migration aplicada nunca é editada;** correções são sempre novas migrations. As versões usam data e hora, para evitar colisões entre módulos.

### 5.2 Modelagem dos eventos

Os eventos usam um modelo híbrido:

- uma **tabela base de eventos**, com as colunas comuns (identificador, organização, lote, tipo, momento da medição, momento do recebimento, responsável, referência à retificação) e o conteúdo específico de cada tipo em JSONB, acompanhado de um número de versão;
- uma **tabela dedicada de medições**, com colunas tipadas (métrica, valor e unidade informados, valor canônico), porque é ela que alimenta as curvas e a comparação.

A linha do tempo do lote lê a tabela de eventos; as curvas leem a tabela de medições. Uma visão filtra os eventos que foram retificados.

**Evolução do conteúdo dos eventos:** como eventos são imutáveis, registros antigos permanecem com o formato antigo. O número de versão permite que a camada de persistência converta formatos antigos para o formato atual na leitura, evitando que uma mudança de código quebre o histórico.

### 5.3 Identificadores e concorrência

- Identificadores **UUIDv7**, gerados no cliente (necessário para o offline) e ordenados no tempo, o que preserva a eficiência dos índices.
- Tabelas mutáveis (lote, recipiente) usam **lock otimista**: uma alteração concorrente recebe erro de conflito em vez de sobrescrever a outra silenciosamente.
- Todas as datas são `timestamptz`, e todas as tabelas têm colunas de auditoria (criado em, criado por).

### 5.4 Materializações (modelo de leitura)

As telas mais acessadas nunca percorrem o histórico de eventos. Elas leem tabelas pequenas, mantidas sempre atualizadas:

| Materialização | Conteúdo | Usada por |
|---|---|---|
| Situação do lote | uma linha por lote: status, recipiente atual, instante zero, volume e número de bateladas de mosto, OG calculada (média ponderada das bateladas), OG medida (tanque cheio) e OG efetiva, últimas leituras, atenuação aparente, último evento | painel da adega e lista de lotes |
| Ocupação dos recipientes | uma linha por tanque: lote atual (id e código) e desde quando | quadro de tanques e listagem de recipientes |

**O que deliberadamente não é materializado:** o tempo relativo à inoculação de cada medição. Ele é calculado na consulta (`ocorrido_em − instante zero do lote`), uma subtração barata que usa o mesmo índice. Guardá-lo em cada medição obrigaria a recalcular todas as medições do lote sempre que a data da inoculação fosse corrigida. Uma materialização só se justifica quando o custo de ler sem ela é maior que o custo de mantê-la.

**Como cada mecanismo é usado:**

- **Materializações de negócio** são atualizadas pela aplicação, **na mesma transação** em que o evento é gravado. A lógica fica explícita, testável e acompanha o módulo se ele for extraído.
- **Triggers** são reservados para **invariantes técnicas**: impedir alteração ou exclusão de eventos, preencher datas de atualização e alimentar a trilha de auditoria.
- **Visões materializadas** do PostgreSQL ficam para relatórios futuros que toleram alguns minutos de defasagem.

### 5.5 Exclusão de dados

| Tipo de dado | Estratégia |
|---|---|
| Eventos | Nunca são excluídos nem alterados. Correção por retificação, com bloqueio garantido por trigger. |
| Cadastros (recipientes, usuários) | Inativação lógica com data e responsável. |
| Entidades com ciclo de vida (lote) | O próprio status representa a inativação (descartado, arquivado). |
| Dados técnicos temporários | Exclusão física por rotina de limpeza (por exemplo, eventos do outbox já publicados). |

Cuidados associados à inativação lógica:

- **Restrições de unicidade** valem apenas entre registros ativos (índices únicos parciais).
- **Consultas padrão** excluem inativos automaticamente; incluí-los exige uma escolha explícita no código.
- **Índices** das consultas do dia a dia cobrem apenas registros ativos.
- **Dados pessoais** de usuários removidos são **anonimizados**, e não apenas inativados, em respeito à LGPD. A trilha de auditoria continua íntegra.

### 5.6 Saúde do banco no longo prazo

- Monitoramento de consultas (`pg_stat_statements`) ativo desde o início.
- Toda consulta nova chega à revisão acompanhada do seu plano de execução.
- **Toda listagem é paginada**, sempre por *keyset*, nunca por deslocamento.
- Consultas que trazem dados relacionados são feitas de uma vez, sem o problema de N+1.
- Uma política de retenção e agregação de dados brutos será definida **antes** da chegada dos sensores.
- O pool de conexões é dimensionado conscientemente.

---

## 6. Offline e sincronização (mobile)

**Escopo:** o offline vale apenas no aplicativo mobile e apenas para o **registro de eventos de adega**. Cadastros, criação de lotes e comparações exigem conexão. Web e desktop funcionam conectados.

### Fluxo

1. O operador registra o evento, que é validado no próprio aparelho.
2. O evento vai para uma **fila local** (SQLite) com seu identificador e o momento da medição, e aparece imediatamente na tela, marcado como pendente.
3. Quando houver rede (ao reconectar, ao abrir o app ou por ação manual), a fila é enviada ao servidor em ordem.
4. O servidor responde **individualmente** para cada evento: aceito, duplicado (tratado como sucesso) ou rejeitado, com o motivo.
5. Eventos aceitos saem da fila. Eventos rejeitados vão para a **caixa de conflitos**.

### Garantias

- **Idempotência:** reenviar o mesmo evento não gera duplicidade. Um mesmo identificador com conteúdo diferente indica defeito no cliente e é registrado como erro.
- **Sucesso parcial:** a rejeição de um evento não afeta os demais do mesmo envio.
- **Ordem:** os eventos são processados na ordem em que foram criados, porque uma retificação pode se referir a um evento do mesmo envio.
- **Nenhum registro se perde por expiração de sessão:** se o login expirar durante o offline, a fila é preservada até que o usuário se autentique novamente.
- **Transparência:** o número de eventos pendentes está sempre visível, e há um aviso quando algo permanece pendente por tempo demais. Nenhum conflito desaparece sem decisão do usuário.

O mecanismo de sincronização é um módulo TypeScript independente, com rede e relógio injetados, testável sem emulador. Optamos por uma implementação própria, em vez de motores de sincronização bidirecional prontos, porque o problema aqui é uma fila de saída que só recebe inclusões, muito mais simples. Essa escolha merece um registro de decisão.

---

## 7. Logs e observabilidade

- O código registra logs apenas por meio da fachada **SLF4J**; o **Log4j2** é a implementação. A troca de implementação não afeta nenhuma classe.
- **Desenvolvimento:** todas as consultas SQL são registradas com os valores preenchidos, prontas para copiar e executar.
- **Produção:** apenas consultas lentas são registradas, com o tempo de execução e **sem os valores**, para não expor dados sensíveis.
- Logs estruturados em JSON em produção, com um **identificador de correlação** por requisição, inclusive nas sincronizações vindas do celular.
- Métricas via Actuator e Micrometer, com Prometheus e Grafana opcionais no ambiente local.

---

## 8. Internacionalização

O app suporta **português (Brasil), espanhol e inglês** desde o início.

- **Textos de interface** são identificados por tokens com uma convenção de nomes única.
- **Valores de domínio** (tipos de evento, off-flavors, status) são gravados como códigos e traduzidos na exibição.
- **O backend não traduz nada.** Erros são devolvidos com um código e parâmetros, no formato *Problem Details*, e o cliente exibe a mensagem traduzida.
- **Números e datas** seguem o formato do idioma, com atenção especial à entrada de densidade, em que vírgula e ponto decimal geram confusão.
- **Unidades de medida** são uma preferência do usuário, independente do idioma.
- Conteúdo digitado pelos usuários não é traduzido.

**Onde as traduções vivem:** no MVP, em arquivos JSON versionados junto com o código, embutidos nos aplicativos (o que resolve o offline). As chaves são tipadas, e o CI falha se faltar uma tradução em algum idioma. Quando houver pessoas que não são desenvolvedoras editando traduções, elas migram para o banco, com uma tela de administração e publicação em CDN. A convenção de nomes já está preparada para essa migração.

---

## 9. Segurança

O princípio geral é a **defesa em profundidade**: cada camada cobre a eventual falha da anterior.

### 9.1 Autenticação

O login é delegado a um **provedor de identidade (Keycloak)** via OpenID Connect. Os clientes usam o fluxo *Authorization Code* com PKCE, e o backend apenas **valida os tokens** recebidos (assinatura, emissor, destinatário e validade). O sistema **não armazena nenhuma senha**, nem em forma de hash. Recuperação de senha, MFA, proteção contra força bruta e login social vêm do provedor.

Como o backend depende apenas do padrão OpenID Connect, trocar o Keycloak por Cognito ou outro provedor é uma mudança de configuração, não de código.

### 9.2 Autorização

- **Negação por padrão:** toda rota exige autenticação. A lista de rotas públicas é explícita e mínima (verificação de saúde e, apenas em desenvolvimento, a documentação da API).
- **Toda operação declara a permissão que exige.** Estar autenticado não basta.
- **O provedor de identidade diz quem a pessoa é; o banco do BrewApp diz o que ela pode fazer.** Papéis ficam no vínculo do usuário com a organização (dono, cervejeiro, operador, leitor), e mudanças valem imediatamente.
- **A organização em uso é informada por cabeçalho e validada contra os vínculos do usuário** a cada requisição. Uma organização informada no corpo da requisição é ignorada.
- As verificações de permissão acontecem nos **casos de uso**, para que nenhum caminho alternativo de execução as contorne.
- **O build falha se uma rota ficar desprotegida:** uma regra de arquitetura exige a declaração de permissão em todo endpoint, e um teste percorre todas as rotas verificando as respostas sem autenticação e sem permissão.

### 9.3 Isolamento entre organizações

1. Todos os repositórios filtram pela organização.
2. Chaves estrangeiras compostas impedem, no próprio banco, que um registro de uma organização referencie outro de uma organização diferente.
3. **Row Level Security** do PostgreSQL como última barreira. A aplicação se conecta com um usuário que não é dono das tabelas, e a organização é definida **por transação**, o que evita que o contexto de uma requisição vaze para outra pelo pool de conexões.

### 9.4 Proteção de dados

| Dado | Proteção |
|---|---|
| Credenciais de login | Não ficam no BrewApp; ficam no provedor de identidade. |
| Dados pessoais (nome, e-mail) | Mínimo necessário; anonimização na remoção (LGPD). |
| Receitas e dados de processo (segredo industrial) | Isolamento por organização e criptografia do armazenamento. |
| Segredos de integrações futuras (sensores, webhooks) | Criptografia na aplicação, com chave gerenciada fora do banco (KMS). |

- Criptografia **por coluna** é usada apenas para segredos, já que impede filtros e índices. O restante é protegido pela criptografia do armazenamento.
- TLS em todas as comunicações.
- Segredos nunca vão para o repositório: arquivos locais ignorados pelo Git, gerenciador de segredos em produção e varredura automática de segredos em cada commit.
- Logs nunca contêm dados pessoais nem tokens.

### 9.5 Clientes e API

- Validação de formato na entrada e de regras no domínio; erros sem detalhes internos.
- Limite de requisições, com atenção especial à sincronização.
- CORS restrito às origens conhecidas, incluindo a do desktop.
- Tokens guardados no armazenamento seguro do sistema no mobile e no desktop, e em memória na web. Na web, a evolução para o padrão BFF (token guardado no servidor e cookie protegido no navegador) fica prevista para depois do MVP.
- Trilha de auditoria para todas as alterações de cadastro.

### 9.6 Dependências e cadeia de suprimentos

Atualização automática de dependências, análise estática de código, varredura de vulnerabilidades nas imagens e varredura de segredos, todas no CI e bloqueando o build em caso de problema crítico.

### 9.7 Modelo de ameaças

Um documento curto de modelo de ameaças listará os ativos (receitas, dados pessoais), os possíveis atacantes (outra organização, operador mal-intencionado, celular roubado) e qual camada protege contra cada um.

---

## 10. Testes e qualidade

| Nível | Abordagem |
|---|---|
| Domínio | Testes unitários puros, sem framework nem banco. Regras e cálculos desenvolvidos com TDD. |
| Casos de uso | Repositórios simulados em memória; mocks apenas para dependências externas. |
| Persistência | PostgreSQL real em container, incluindo Row Level Security, triggers e índices. Nunca um banco em memória substituto. |
| Módulos | Testes por módulo e verificação automática das fronteiras. |
| API e segurança | Varredura de rotas e **teste de isolamento entre organizações**, considerado o teste mais importante do sistema. |
| Arquitetura | Regras automáticas: o domínio não depende de framework, todo endpoint declara permissão. |
| Contratos | Tipos da API sempre atualizados; casos de teste compartilhados entre Java e TypeScript. |
| Front-end e mobile | Testes de componentes e do mecanismo de sincronização. |
| Ponta a ponta | Cypress nos fluxos críticos da web. |

**Princípios:**

- Todo código que depende do tempo recebe o relógio por injeção, o que mantém os testes determinísticos.
- Meta de cobertura alta **no domínio e nos casos de uso**, sem meta global de vaidade. Mutation testing no domínio como medida adicional da qualidade dos testes.
- Pipelines de CI separados por área do monorepo.
- Formatação automática aplicada **apenas nos arquivos alterados**.

---

## 11. Decisões a registrar (ADRs)

Cada decisão abaixo será registrada em um ADR próprio:

1. Monolito modular com Spring Modulith e extração planejada de serviços.
2. jOOQ com abordagem *schema-first* em vez de JPA.
3. Arquitetura hexagonal por módulo.
4. Diário de eventos sem Event Sourcing.
5. Modelo híbrido de eventos (tabela base com JSONB e tabela dedicada de medições).
6. Materializações mantidas pela aplicação; triggers apenas para invariantes técnicas.
7. Estratégias de exclusão por tipo de dado.
8. Offline restrito ao registro de eventos, com sincronização própria.
9. Regra de aceitação de eventos atrasados pelo momento da medição.
10. Autenticação delegada a provedor OpenID Connect (Keycloak), com troca revisável.
11. Isolamento entre organizações em três camadas, incluindo Row Level Security.
12. Internacionalização em arquivos versionados, com migração planejada para o banco.
13. Contrato da API gerado a partir do código.
14. Log4j2 por trás da fachada SLF4J, com política de log de SQL por ambiente.
15. Exceções à regra de paginação: vínculos no `GET /me` (coleção pequena por natureza) e comparação de lotes (resposta limitada por agregação em vez de paginação).
16. Direção única de dependência entre módulos, com chamadas síncronas na mesma transação e inversão de dependência para a ocupação dos tanques.

---

## 12. Questões em aberto

- **Infraestrutura de produção:** provedor de nuvem e forma de deploy ainda não definidos. A escolha impacta o provedor de identidade definitivo e o gerenciamento de segredos.
- **Conteúdo exato do plano importado do BeerXML:** quais campos do arquivo serão aproveitados no MVP.

Questões resolvidas no design da API (`docs/api/design-api.md`): lista de off-flavors do MVP (seção 5.2) e limites das sinalizações de relógio (seção 5.5, configuráveis).
