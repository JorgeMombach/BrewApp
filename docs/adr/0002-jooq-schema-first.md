# ADR 0002: jOOQ com abordagem schema-first em vez de JPA

- **Status:** aceito
- **Data:** 07/10/2026
- **História:** BREW-15
- **Referências:** spec do MVP, seções 4.3, 5.1, 5.5 e 9.3

## Contexto

O BrewApp é, antes de tudo, um sistema de banco de dados. O diário de adega depende de recursos do PostgreSQL que um ORM esconde ou atrapalha: Row Level Security para isolar as organizações, triggers que impedem alterar eventos, JSONB no conteúdo dos eventos, índices parciais para a inativação lógica e consultas com CTEs para as curvas e a comparação de lotes. Cada consulta nova também precisa chegar à revisão com o seu plano de execução, o que exige saber exatamente qual SQL vai rodar.

Com JPA, o modelo de objetos manda no banco, o SQL é gerado em tempo de execução e problemas como N+1 só aparecem quando a aplicação já está rodando. Além disso, as entidades gerenciadas tendem a vazar para o domínio, o que conflita com a arquitetura hexagonal, em que o domínio é Java puro.

## Decisão

**O banco é a fonte da verdade, e o código de acesso a dados é gerado a partir dele.**

1. **Flyway aplica as migrations, e o jOOQ gera as classes a partir do banco migrado.** O SQL é escrito de forma explícita e tipada, e uma mudança no schema que quebre uma consulta quebra a compilação.
2. **O código gerado é versionado.** Ele fica numa pasta própria do backend, marcada como gerada para o GitHub colapsá-la no diff. O projeto compila logo após o clone, sem Docker, e a pull request mostra o impacto de cada migration.
3. **A geração fica fora do build padrão.** Ela roda num profile do Maven, acionado por quem muda uma migration. O profile sobe um PostgreSQL descartável com Testcontainers, com as mesmas roles do ambiente local, aplica as migrations com o Flyway e gera as classes.
4. **Uma checagem de divergência garante que o código versionado bate com as migrations.** Ela regenera o código e falha se houver qualquer diferença. Até a entrega da pipeline de CI (BREW-20), a checagem é um passo local antes de abrir a pull request; depois, vira um job da CI.
5. **As classes de cada schema são geradas dentro da camada de persistência do próprio módulo.** Assim, a verificação de fronteiras do Spring Modulith impede que um módulo use as tabelas de outro, e uma regra de arquitetura impede que os tipos do jOOQ saiam da camada de persistência.
6. **Migrations por módulo, versionadas por data e hora.** Cada módulo tem a sua pasta, e a versão segue o formato `V<AAAAMMDD>_<HHMM>__descricao.sql`. Duas migrations nunca têm a mesma data e hora, mesmo em pastas diferentes. Migration aplicada nunca é editada: a correção é sempre uma migration nova.
7. **Um histórico único do Flyway,** num schema próprio. Os schemas dos módulos são criados pelas próprias migrations.
8. **Duas roles no banco.** A role de migração é dona dos schemas e das tabelas e só é usada pelo Flyway. A role da aplicação lê, insere e altera dados por privilégios padrão de cada schema, mas não cria nem altera estruturas. A exclusão de linhas é concedida tabela a tabela, porque exclusão física é exceção no modelo de dados. Isso é pré-requisito do Row Level Security, que não se aplica ao dono da tabela.

### Como regenerar e checar a divergência

A partir da pasta do backend, com o Docker rodando:

```bash
./mvnw -Pjooq-codegen generate-sources
git status --porcelain -- src/main/generated
```

A segunda linha não pode imprimir nada. Se imprimir, o código gerado não acompanha as migrations: o que foi regenerado deve ser commitado junto com a migration.

## Consequências

**Positivas**

- Divergência entre banco e código aparece na compilação ou na checagem de divergência, não em produção.
- O SQL que roda é o SQL que está no código, o que facilita revisar planos de execução e evita N+1 por acidente.
- Os recursos do PostgreSQL ficam disponíveis sem contorno.
- O isolamento entre módulos vale também para o acesso a dados.

**Negativas**

- Mais código para escrever do que com repositórios do Spring Data: as conversões entre records do jOOQ e o domínio são feitas à mão no adapter de persistência.
- Quem muda uma migration precisa lembrar de regenerar o código. A checagem de divergência cobre o esquecimento, mas só passa a ser automática com a CI.
- A configuração do Flyway existe em dois lugares (na aplicação e no profile de geração) e precisa ser mantida igual. A imagem do PostgreSQL também aparece no docker-compose, nos testes e no profile.
- A geração precisa de Docker.

## Alternativas consideradas

- **JPA/Hibernate:** descartado pelos motivos do contexto.
- **Código gerado fora do Git, gerado a cada build:** era a decisão original da spec. Garante a sincronia sem checagem extra, mas exige Docker até para compilar e esconde da pull request o impacto das migrations. Foi trocada pela versionada em 07/10/2026 (decisão registrada no plano do Sprint 1).
- **Plugin pronto do Testcontainers para codegen do jOOQ:** sem atualização desde 2024 e incompatível com as versões atuais do Flyway e do jOOQ. Foi substituído por plugins que o Spring Boot já gerencia, com um script curto para subir o container.
- **Um histórico do Flyway por módulo:** facilitaria a extração de um serviço, mas complica a ordem das migrations e a configuração sem ganho no monolito. Pode ser revisto na primeira extração.
