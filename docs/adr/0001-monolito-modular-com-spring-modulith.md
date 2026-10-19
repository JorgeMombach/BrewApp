# ADR 0001: Monolito modular com Spring Modulith e extração planejada de serviços

- **Status:** aceito
- **Data:** 19/10/2026
- **História:** BREW-22 (decisão em vigor desde o esqueleto do backend, BREW-14, e registrada depois)
- **Referências:** spec do MVP, seções 4.1, 4.2, 4.4 e 5.1; ADR 0002; ADR 0003; ADR 0016 (previsto)

## Contexto

O BrewApp tem domínios com fronteiras claras: organizações e usuários, recipientes, lotes, o diário de adega e a comparação de lotes. O produto ainda não entregou o primeiro valor, é desenvolvido por uma pessoa e roda num só ambiente. Mesmo assim, o modelo de dados já está preparado para várias organizações, e um SaaS é uma possibilidade.

Microsserviços desde o primeiro dia trariam custos antes de qualquer benefício: consistência eventual entre serviços, transações distribuídas, um banco, uma pipeline e um deploy por serviço, e chamadas de rede onde hoje basta uma chamada de método. Por outro lado, um monolito sem fronteiras tende a virar um emaranhado em que tudo depende de tudo, e aí extrair qualquer parte fica caro.

Também é preciso que as fronteiras se sustentem sozinhas. Uma convenção escrita só se mantém enquanto todos lembram dela, e o projeto quer que um descuido apareça no build, não numa revisão.

## Decisão

**O backend é um único serviço, dividido em módulos com fronteiras verificadas pelo Spring Modulith a cada build, e preparado para extrair serviços quando houver um motivo real.**

1. **Um módulo por domínio.** Organização, Recipiente, Lote, Adega e Comparação, cada um num pacote direto de `dev.brewapp`, mais o núcleo compartilhado (`shared`), que fica com o mínimo possível: tipos de valor como quantidade e unidade, os códigos de erro e a infraestrutura comum (web, segurança, logs, relógio).
2. **Fronteiras verificadas no build.** Um teste de arquitetura roda a verificação do Spring Modulith sobre a aplicação inteira. Ele falha quando um módulo usa um pacote interno de outro ou quando surge uma dependência circular. Só o pacote raiz de cada módulo e as interfaces nomeadas declaradas por ele são públicos.
3. **Um schema do PostgreSQL por módulo, e nenhum módulo lê as tabelas de outro.** As migrations ficam numa pasta por módulo, e as classes do jOOQ de cada schema são geradas dentro da persistência do próprio módulo (ADR 0002). Com isso, a verificação de fronteiras vale também para o acesso a dados. Módulos sem dados próprios, como a Comparação, não têm schema.
4. **Comunicação entre módulos.** Chamadas síncronas pela interface pública do módulo, na mesma transação, quando a operação precisa de resposta ou de atomicidade. A direção das dependências e a inversão para a ocupação dos tanques ficam no ADR 0016, previsto para a primeira chamada entre módulos.
5. **Eventos de domínio só para quem apenas reage.** Um módulo publica fatos ("inoculação registrada") para consumidores que não devolvem nada ao publicador. Os eventos são gravados na mesma transação do caso de uso, pelo registro de publicação de eventos do Spring Modulith (padrão outbox), para nenhum se perder. A dependência entra no projeto com o primeiro evento publicado.
6. **Extração planejada, não antecipada.** O primeiro candidato é o módulo de Alertas e agendamentos: é assíncrono por natureza, tem ciclo de vida próprio e só consome eventos dos lotes. Na extração, os eventos do outbox passam a ser publicados no RabbitMQ, quase sem mudança no domínio. Um módulo só vira serviço com um motivo concreto, como escala, ritmo de deploy diferente ou isolamento de falhas.

## Consequências

**Positivas**

- Um deploy, uma pipeline e um banco: o custo operacional é o de uma aplicação só.
- Uma operação que envolve dois módulos roda numa transação local, sem saga nem compensação.
- Fronteira violada ou ciclo entre módulos quebra o build, e não depende de alguém perceber na revisão.
- Com schemas separados e eventos já gravados no outbox, a extração de um módulo é uma mudança localizada, e não uma reescrita.
- A documentação dos módulos pode ser gerada pelo próprio Spring Modulith a partir do código, quando for útil.

**Negativas**

- Os módulos escalam juntos: um módulo pesado consome recursos de todos.
- Uma falha grave (memória esgotada, por exemplo) derruba todos os módulos de uma vez.
- A disciplina de fronteiras tem custo agora: uma informação de outro módulo exige uma interface pública ou uma porta, mesmo quando uma consulta direta à tabela seria mais curta.
- Schemas separados impedem junções entre módulos no banco. A comparação de lotes e telas que juntam dados de vários módulos montam o resultado chamando as interfaces públicas, ou usam materializações dentro do próprio módulo.
- O histórico do Flyway é único (ADR 0002). Na primeira extração, as migrations do módulo extraído precisam ser separadas.

## Questão em aberto

**Como os módulos de negócio usam o `shared`.** Hoje, para o Spring Modulith, os subpacotes do `shared` são internos. Só o pacote raiz (códigos de erro, `ErroDeNegocio`, cabeçalhos) e o catálogo de unidades, declarado como interface nomeada porque o domínio usa `Quantidade`, ficam visíveis aos outros módulos. A primeira história de negócio decide entre declarar interfaces nomeadas para o que mais for preciso ou marcar o `shared` como módulo aberto. A decisão revisa este ADR.

## Alternativas consideradas

- **Microsserviços desde o início:** descartados pelos custos do contexto, pagos antes de o produto existir e sem um problema de escala que os justifique.
- **Monolito sem módulos, organizado por camada técnica (controllers, services, repositories):** é o mais rápido no começo, mas espalha cada domínio pelo projeto inteiro e não impede dependências cruzadas. Extrair um serviço depois exigiria desembaraçar o código primeiro.
- **Módulos Maven separados, um artefato por domínio:** as fronteiras ficariam no compilador, mas o build ficaria mais pesado e a configuração repetida em vários `pom.xml`, sem ganho sobre a verificação do Spring Modulith num artefato só. Pode ser revisto se o build ficar lento.
- **Java Platform Module System (JPMS):** dá fronteiras no compilador, mas convive mal com a reflexão do Spring e das bibliotecas de teste, e o Spring Modulith já cobre a necessidade.
- **Fronteiras só por convenção e revisão, sem verificação automática:** contraria a premissa de que uma regra só se mantém se o build a fizer valer.
