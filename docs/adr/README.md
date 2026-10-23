# Registros de decisão de arquitetura (ADRs)

Cada ADR registra uma decisão de arquitetura do BrewApp: o contexto, o que foi decidido, as consequências aceitas e as alternativas descartadas. Quem for mudar uma dessas decisões deve começar pelo ADR correspondente, para entender o raciocínio antes de propor outro.

Para escrever um novo, use o [template](template.md).

## Regras

- **Numeração.** Os números de 1 a 16 seguem a lista de decisões da spec do MVP (seção 11), e não a ordem em que foram escritos. Uma decisão fora dessa lista pega o próximo número livre e entra também na lista da spec.
- **Quando escrever.** Na mesma entrega que introduz ou altera a decisão, como pede a Definition of Done. Os previstos abaixo indicam a epic onde a decisão deve aparecer pela primeira vez, mas o que manda é a história que a introduzir.
- **Status.** `proposto` enquanto a pull request está aberta; `aceito` no merge. Ajustes pequenos entram como revisão no próprio ADR, anotados no cabeçalho. Uma troca de decisão vira um ADR novo, e o antigo passa a `substituído pelo ADR NNNN`, sem ser apagado.

## Índice

| Nº | Decisão | Status | História ou epic |
|---|---|---|---|
| 0001 | [Monolito modular com Spring Modulith e extração planejada de serviços](0001-monolito-modular-com-spring-modulith.md) | aceito | BREW-22 |
| 0002 | [jOOQ com abordagem schema-first em vez de JPA](0002-jooq-schema-first.md) | aceito | BREW-15 |
| 0003 | [Arquitetura hexagonal por módulo](0003-arquitetura-hexagonal-por-modulo.md) | aceito | BREW-22 |
| 0004 | Diário de eventos sem Event Sourcing | previsto | BREW-9 Diário de adega |
| 0005 | Modelo híbrido de eventos (tabela base com JSONB e tabela dedicada de medições) | previsto | BREW-9 Diário de adega |
| 0006 | Materializações mantidas pela aplicação; triggers apenas para invariantes técnicas | previsto | BREW-9 Diário de adega |
| 0007 | Estratégias de exclusão por tipo de dado | previsto | Primeiro cadastro com inativação lógica: BREW-6 Identidade e organização ou BREW-7 Recipientes |
| 0008 | Offline restrito ao registro de eventos, com sincronização própria | previsto | BREW-10 Mobile e offline |
| 0009 | Regra de aceitação de eventos atrasados pelo momento da medição | previsto | BREW-9 Diário de adega |
| 0010 | [Autenticação delegada a provedor OpenID Connect (Keycloak), com troca revisável](0010-autenticacao-delegada-a-provedor-oidc.md) | proposto | BREW-23 |
| 0011 | Isolamento entre organizações em três camadas, incluindo Row Level Security | previsto | BREW-6 Identidade e organização |
| 0012 | [Internacionalização em arquivos versionados, com migração planejada para o banco](0012-internacionalizacao-em-arquivos-versionados.md) | aceito | BREW-19 |
| 0013 | [Contrato da API gerado a partir do código](0013-contrato-da-api-gerado-a-partir-do-codigo.md) | aceito | BREW-18 |
| 0014 | [Log4j2 por trás da fachada SLF4J, com política de log de SQL por ambiente](0014-log4j2-slf4j-e-politica-de-log.md) | aceito | BREW-16, revisado na BREW-69 |
| 0015 | Exceções à regra de paginação: vínculos no `GET /me` e comparação de lotes | previsto | BREW-6 Identidade e organização (`GET /me`), completado na BREW-11 Comparação de lotes |
| 0016 | Direção única de dependência entre módulos, com chamadas síncronas na mesma transação e inversão de dependência para a ocupação dos tanques | previsto | BREW-9 Diário de adega (primeira chamada entre módulos) |
| 0017 | [Cliente HTTP tipado pelo contrato, com toda falha entregue como ErroDaApi](0017-cliente-http-tipado-e-erros-como-erro-da-api.md) | aceito | BREW-17 |
