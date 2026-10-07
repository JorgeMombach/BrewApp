# Definition of Done

Este documento define quando uma história, task ou bug do BrewApp está pronto. A ideia é que segurança, qualidade e internacionalização façam parte de toda entrega, e não virem tarefas acumuladas para o fim do projeto.

Os critérios se dividem em dois grupos. Os **gerais** valem para toda entrega. Os **por tipo de mudança** valem só quando a história mexe naquela área: uma história de infraestrutura não tem endpoint e, por isso, não precisa de teste de isolamento entre organizações. Quando um critério não se aplica, isso é dito na pull request, com o motivo em poucas palavras.

O checklist do template de pull request segue este documento. Se um dos dois mudar, o outro muda junto.

## Quando uma história está pronta

Uma história está **pronta** quando é mergeada na branch da sua epic (`epic/BREW-<n>`) com a CI verde e atende a todos os critérios que se aplicam a ela.

A chegada da epic à `main` é uma entrega de release. Ela não é condição para concluir a história no sprint.

> **Exceção temporária:** enquanto a BREW-20 (pipeline de CI no GitHub Actions) não for entregue, a história é considerada pronta quando mergeada na epic, sem exigir CI verde. Nesse período, o build e os testes precisam passar localmente antes do merge. A decisão está registrada no plano do Sprint 1, em 03/10/2026.

## Critérios gerais

Valem para toda entrega.

1. **Critérios de aceite conferidos um a um** com o que está no Jira. Se um critério mudou durante o trabalho, o Jira é atualizado.
2. **Build completo passando**, incluindo os testes, a verificação das fronteiras entre módulos e as regras de arquitetura.
3. **Checagem de regressão feita e descrita na pull request:**
   - quem consome o que foi alterado;
   - quais fluxos são afetados;
   - condicionais que mudam o caminho, como permissões e configurações por ambiente;
   - dados que já existem no banco ou no celular.

   A descrição separa o que foi verificado do que ficou como suposição.
4. **Nenhum dado pessoal ou token nos logs.** Isso inclui mensagens de exceção e valores de consultas SQL fora do ambiente de desenvolvimento.
5. **Nada sensível nos commits.** A varredura de segredos do pre-commit não pode ser desligada ou contornada.
6. **Formatação aplicada só nos arquivos alterados.**
7. **Documentação acompanha a decisão.** Se a história muda algo definido na spec do MVP, no design da API ou no fluxo de branches, o documento é atualizado na mesma entrega, assim como a issue no Jira.
8. **Horas registradas na história** pelo `#time` nos commits, no padrão definido no fluxo de branches.

## Critérios por tipo de mudança

### Regras de negócio

- Testes unitários do domínio, puros, sem framework nem banco. Regras e cálculos são desenvolvidos com TDD.
- Testes dos casos de uso com repositórios falsos em memória. Mocks só para dependências externas.
- Código que depende do tempo recebe o relógio por injeção. Nada de ler a hora do sistema diretamente.
- Erros de negócio usam o código do enum do módulo, nunca um texto solto.

### Persistência

- Testes de integração com PostgreSQL real em container, cobrindo também Row Level Security, triggers e índices quando a história os cria ou altera. Banco em memória substituto não serve.
- Toda tabela de negócio nova tem `organization_id`, chaves estrangeiras compostas com a organização e política de Row Level Security.
- Os tipos gerados pelo jOOQ não saem da camada de persistência.

### Migrations e índices

- Toda mudança de banco é uma migration nova. Uma migration que já foi mergeada em uma epic nunca é editada: a correção vira outra migration.
- Índice novo, ou consulta nova que dependa de um índice, vem com a saída de `EXPLAIN (ANALYZE, BUFFERS)` na descrição da pull request. A saída informa com quantas linhas a tabela estava, porque um plano gerado com uma tabela quase vazia não mostra se o índice está sendo usado.

### Endpoints

- A rota exige autenticação e declara a permissão que exige. Estar autenticado não basta.
- A rota é coberta pelo teste de isolamento entre organizações: acessar um recurso de outra organização responde 404.
- Toda listagem é paginada por keyset. As únicas exceções são as já registradas (`GET /me` e `GET /comparacoes`). Uma exceção nova exige ADR.
- Erros saem no formato Problem Details, com código estável. Um código novo ou renomeado é mudança de contrato: o design da API e o teste de contrato dos códigos de erro mudam na mesma entrega.
- Recurso mutável devolve `ETag` na leitura e exige `If-Match` na alteração.
- Quando houver endpoint novo ou alterado, o contrato da API e os tipos TypeScript compartilhados são atualizados na mesma entrega.

### Interface (web, desktop e mobile)

- Nenhum texto fixo na interface. Toda chave de tradução existe nos três idiomas: português (Brasil), espanhol e inglês.
- Valores de domínio, como tipos de evento e status, são exibidos a partir do código traduzido.
- Números e datas seguem o formato do idioma. A entrada de densidade merece atenção especial por causa da vírgula e do ponto decimal.
- A lógica de tela e o mecanismo de sincronização têm testes de componente quando a história mexe neles.

### Decisões de arquitetura

- Quando a história introduz ou altera uma decisão de arquitetura, um ADR é escrito em `docs/adr/` na mesma entrega.

### Correção de bug

- Um teste reproduz o bug antes da correção e passa depois dela.

## Como registrar na pull request

A pull request usa o template do repositório, com este checklist:

- cada critério é marcado como feito ou como "não se aplica", com o motivo;
- a checagem de regressão e, quando houver, a saída do `EXPLAIN` ficam na descrição.

## Mudanças neste documento

O DoD muda por decisão registrada na seção "Decisões durante o sprint" do plano do sprint em andamento. O template de pull request é atualizado na mesma entrega.
