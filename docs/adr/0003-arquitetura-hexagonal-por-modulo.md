# ADR 0003: Arquitetura hexagonal por módulo

- **Status:** aceito
- **Data:** 19/10/2026
- **História:** BREW-22 (decisão em vigor desde o esqueleto do backend, BREW-14, e registrada depois)
- **Referências:** spec do MVP, seções 4.3 e 10; ADR 0001; ADR 0002

## Contexto

O ADR 0001 divide o backend em módulos por domínio. Falta decidir como cada módulo se organiza por dentro.

O valor do BrewApp está nas regras de domínio: o ciclo de vida do lote, a aceitação de eventos atrasados, a retificação, as conversões de unidade e o tempo relativo à inoculação. Essas regras precisam ser testadas a fundo, de preferência com TDD, e isso só é barato quando elas rodam sem subir o Spring nem um banco.

Ao mesmo tempo, o acesso a dados usa jOOQ com SQL explícito (ADR 0002), e as classes geradas espelham as tabelas. Se essas classes chegarem às regras de negócio, o domínio passa a ter o formato do banco, e uma mudança de schema se espalha pelo módulo inteiro. O mesmo vale para as classes do Spring e para os tipos da API REST.

## Decisão

**Cada módulo segue a arquitetura hexagonal (portas e adaptadores), com o domínio em Java puro no centro e as conversões entre representações só nas fronteiras.**

1. **Quatro camadas por módulo, em pacotes com os mesmos nomes em todos os módulos.**

   | Pacote | Conteúdo | Pode depender de |
   |---|---|---|
   | `domain` | Entidades, value objects e regras de negócio | Só Java e o núcleo compartilhado de domínio |
   | `application` | Casos de uso e as portas de que eles precisam (repositórios, consultas a outros módulos) | `domain` |
   | `adapter.in.web` | Controllers REST: convertem a requisição para o caso de uso e o resultado para a resposta | `application` e `domain` |
   | `adapter.out.persistence` | Implementação das portas com jOOQ e as classes geradas do schema do módulo | `application` e `domain` |

2. **O domínio é Java puro.** Sem Spring, sem jOOQ, sem anotações de framework. O tempo chega como parâmetro ou por um `Clock` recebido, nunca lido do sistema. Vale também para a parte do núcleo compartilhado que o domínio usa: o pacote raiz do `shared` e o catálogo de unidades.
3. **As portas são declaradas por quem as usa.** O caso de uso define a interface do repositório na camada de aplicação, e o adaptador de persistência a implementa. A dependência aponta sempre para dentro: do adaptador para a aplicação, da aplicação para o domínio.
4. **Os tipos do jOOQ não saem da persistência.** Records e tabelas geradas ficam no adaptador de saída, que converte de e para o domínio. Os casos de uso, os controllers e o domínio nunca os veem.
5. **Conversões só nas fronteiras, com `record`.** Requisição e resposta da API são `record` no adaptador web; a conversão para o domínio acontece ali, e a da persistência no adaptador de saída. Não há uma camada de DTOs e mappers entre aplicação e domínio sem um motivo concreto.
6. **Testes por camada.** Domínio com testes unitários puros. Casos de uso com repositórios falsos em memória (fakes) no lugar das portas, e mocks só para dependências externas. Persistência só com PostgreSQL real em container, nunca um banco em memória.

### O que o build verifica hoje

As regras de arquitetura (ArchUnit) já fazem valer duas partes desta decisão:

- o domínio, o pacote raiz do `shared` e o catálogo de unidades não dependem de Spring nem de jOOQ;
- os tipos do jOOQ e as classes geradas só aparecem dentro de `adapter.out.persistence`.

A direção entre as camadas (por exemplo, a aplicação não depender de um adaptador, ou o domínio não depender da aplicação) ainda não é verificada, porque não existe nenhum módulo de negócio para a regra analisar, e o ArchUnit falha com uma regra que não encontra classes. A regra de camadas entra na primeira história de negócio, junto com o primeiro módulo.

## Consequências

**Positivas**

- As regras de negócio são testadas em milissegundos, sem Spring nem banco, o que torna o TDD viável no domínio.
- Uma mudança de schema fica contida no adaptador de persistência do módulo; a mudança de um campo da API, no adaptador web.
- Fakes de repositório exercitam os casos de uso com comportamento real (gravar e ler de volta), e não só com respostas combinadas de mocks.
- A mesma estrutura em todos os módulos facilita achar o código e escrever regras de arquitetura que valem para todos.

**Negativas**

- Mais tipos e mais conversões escritas à mão: um mesmo conceito pode existir como record do jOOQ, objeto de domínio e record da API.
- Em operações simples, quase um CRUD, as camadas parecem cerimônia. A regra vale mesmo assim, para o módulo não ficar com dois estilos.
- Cada porta de repositório tem duas implementações para manter (a real e o fake), que precisam se comportar igual.
- Enquanto a regra de camadas não existir, a direção entre aplicação e adaptadores depende da revisão.

## Alternativas consideradas

- **Camadas tradicionais (controller, service, repository) com o domínio anotado:** mais simples no começo, mas o domínio passa a depender do framework e do banco, e os testes de regra de negócio precisam do contexto do Spring.
- **Clean Architecture completa, com uma porta de entrada (interface) por caso de uso e modelos próprios em cada camada:** separa ainda mais, mas multiplica interfaces e mappers sem um segundo adaptador de entrada que justifique. O mobile e o desktop usam a mesma API REST.
- **Usar as classes geradas pelo jOOQ como modelo de domínio:** elimina conversões, mas acopla as regras de negócio ao formato das tabelas, o que o ADR 0002 justamente quer evitar.
- **Mocks no lugar de fakes nos casos de uso:** mais rápidos de escrever, mas testam a interação esperada, e não o resultado; um teste com mock pode passar com um repositório que nunca funcionaria.
