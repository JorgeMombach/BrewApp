## O que muda

<!-- Resumo da entrega e link da issue (BREW-<n>). -->

## Checagem de regressão

<!-- Consumidores do que foi alterado, fluxos afetados, permissões e configurações por ambiente, dados antigos. -->

- **Verificado:**
- **Suposições:**

## Plano de execução

<!-- Só quando houver índice novo ou consulta nova que dependa de um índice: saída do EXPLAIN (ANALYZE, BUFFERS) e quantas linhas a tabela tinha. Caso contrário, apague esta seção. -->

## Definition of Done

<!-- Marque cada item como feito. Quando não se aplicar, escreva "não se aplica" e o motivo. Referência: docs/processo/definition-of-done.md -->

### Gerais

- [ ] Critérios de aceite conferidos um a um com o Jira
- [ ] Build completo passando (testes, fronteiras entre módulos e regras de arquitetura)
- [ ] Checagem de regressão descrita acima
- [ ] Nenhum dado pessoal ou token nos logs
- [ ] Nada sensível nos commits; varredura de segredos não contornada
- [ ] Formatação só nos arquivos alterados
- [ ] Documentação e Jira atualizados quando a decisão mudou
- [ ] Horas registradas com `#time` nos commits

### Regras de negócio

- [ ] Testes unitários do domínio, sem framework nem banco
- [ ] Testes dos casos de uso com repositórios falsos em memória
- [ ] Relógio injetado em todo código que depende do tempo
- [ ] Erros de negócio com código do enum do módulo

### Persistência

- [ ] Testes de integração com PostgreSQL real (RLS, triggers e índices, quando houver)
- [ ] Tabela de negócio nova com `organization_id`, FKs compostas e Row Level Security
- [ ] Tipos do jOOQ restritos à camada de persistência

### Migrations e índices

- [ ] Mudança de banco em migration nova; nenhuma migration já mergeada foi editada
- [ ] Plano de execução na seção acima para índice ou consulta nova

### Endpoints

- [ ] Autenticação exigida e permissão declarada
- [ ] Coberto pelo teste de isolamento entre organizações (404 para outra organização)
- [ ] Listagem paginada por keyset (exceção nova exige ADR)
- [ ] Erros em Problem Details; código novo ou renomeado refletido no design da API e no teste de contrato
- [ ] `ETag` na leitura e `If-Match` na alteração de recurso mutável
- [ ] Contrato da API e tipos TypeScript atualizados

### Interface

- [ ] Nenhum texto fixo; chaves de tradução em pt-BR, es e en
- [ ] Valores de domínio exibidos a partir do código traduzido
- [ ] Números e datas no formato do idioma
- [ ] Testes de componente na lógica de tela ou de sincronização alterada

### Arquitetura e bugs

- [ ] ADR em `docs/adr/` quando há decisão de arquitetura nova ou alterada
- [ ] Bug com teste que o reproduz antes da correção
