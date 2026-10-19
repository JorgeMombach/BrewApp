# ADR 0013: Contrato da API gerado a partir do código

- **Status:** aceito
- **Data:** 13/10/2026
- **História:** BREW-18 (previsto para a BREW-22; escrito junto com a implementação, como os ADRs 0002 e 0014)
- **Referências:** spec do MVP, seções 4.5 e 10; design da API, seções 1.6 e 1.11; ADR 0002

## Contexto

O BrewApp tem três clientes do mesmo backend: o app web, o desktop (o mesmo app web empacotado) e o mobile. Os três montam requisições e leem respostas da API, e um campo renomeado no backend só seria percebido quando alguém abrisse a tela. O design da API (`docs/api/design-api.md`) descreve o contrato em prosa, antes do código, mas não impede essa divergência.

Algumas regras de cálculo também precisam rodar dos dois lados. A conversão entre graus Plato e gravidade específica, por exemplo, é feita no backend, que é a fonte da verdade, e no aparelho, para mostrar o valor convertido enquanto o cervejeiro digita, inclusive sem rede. Duas implementações da mesma fórmula tendem a se afastar com o tempo, e a diferença só aparece quando a tela e o relatório mostram números diferentes.

## Decisão

**O contrato OpenAPI é gerado a partir dos controllers, versionado no pacote compartilhado, e os tipos TypeScript são gerados a partir dele. As regras de cálculo duplicadas são testadas contra os mesmos casos nos dois lados.**

1. **O springdoc gera o documento OpenAPI a partir dos controllers.** O design da API continua sendo escrito antes do código; o documento gerado é a verificação de que o código o seguiu.
2. **Documento e Swagger UI só existem no perfil `dev`.** Fora dele, as rotas não são registradas e caem na negação por padrão. Onde estão ligadas, são públicas, para o Swagger UI abrir sem token; o botão de autorização aceita o token do Keycloak para chamar as rotas protegidas.
3. **Um teste de integração exporta o documento para `packages/shared/openapi/openapi.json`.** O teste sobe a aplicação como os demais testes de integração (PostgreSQL em container, sem Keycloak), lê o documento e o grava com formato estável: chaves em ordem alfabética, indentação de dois espaços, LF e servidor relativo (`/`). Assim, todo build local atualiza o arquivo sem passo manual, e o arquivo só muda quando o contrato muda.
4. **Os tipos TypeScript são gerados pelo `openapi-typescript` e versionados** em `packages/shared/src/api/esquema.gerado.ts`. O pacote exporta apelidos para os tipos mais usados (como `ProblemDetails`), e os clientes consomem o código-fonte, sem build próprio do pacote.
5. **Duas checagens de divergência na CI, no mesmo padrão do jOOQ (ADR 0002).** O job do backend falha se o contrato regravado pelo teste diferir do commitado. O job do contrato regenera os tipos e falha se diferirem dos commitados; em seguida, roda a checagem de tipos e os testes TypeScript do pacote.
6. **O que vale para toda a API é declarado uma vez, fora dos controllers:** a autenticação Bearer (JWT) como exigência global, e os schemas `ProblemDetails` e `ErroDeValidacao` (design da API, seção 1.6), montados com os mesmos nomes de propriedade usados pela classe que produz as respostas de erro. Dois ajustes do springdoc sustentam isso:
   - os schemas são mantidos mesmo sem operação que os referencie;
   - as operações não ganham automaticamente as respostas do tratador global de erros, porque o springdoc as descreveria com o `ProblemDetail` do Spring, sem `code`, `params` nem `correlationId`.
7. **Casos de teste compartilhados ficam em `packages/shared/casos-de-teste/`,** um arquivo JSON por grandeza: `{ grandeza, referencia, casos: [{ descricao, de: { valor, unidade }, para, esperado, tolerancia }] }`. O backend lê a pasta pelo classpath de teste; o TypeScript importa o JSON e o atribui a um tipo, o que faz o compilador conferir a estrutura.
8. **Os valores esperados vêm de uma referência externa, não da própria fórmula.** Para densidade, a tabela de extrato da ASBC. Como a relação entre Plato e SG não é linear e as fórmulas são aproximações da tabela, cada caso traz a sua tolerância: larga o bastante para o arredondamento da tabela e estreita o bastante para acusar um coeficiente errado. Conversão para a mesma unidade devolve o valor sem passar pela canônica, porque as fórmulas de ida e volta não são inversas exatas.
9. **TypeScript 5.9 no monorepo.** O `openapi-typescript` usa a API JavaScript do compilador, que o TypeScript 7 (reescrito em Go) não oferece. A troca de versão espera o gerador suportá-la.

### Como atualizar o contrato localmente

Depois de mudar um controller, a partir da raiz do repositório:

```bash
cd backend && ./mvnw verify && cd ..
pnpm --filter @brewapp/shared gerar:tipos
git status --porcelain -- packages/shared/openapi packages/shared/src/api
```

Os arquivos listados pela última linha são commitados junto com o controller.

## Consequências

**Positivas**

- Um campo renomeado ou removido no backend quebra a checagem de tipos dos clientes, e não a tela em uso.
- A pull request mostra o impacto de cada mudança de contrato no `openapi.json`.
- Backend e aparelho calculam conversões iguais, e um caso novo vale para os dois de uma vez.
- O pacote compila logo após o clone, sem subir o backend.

**Negativas**

- Mais artefatos gerados no repositório, que precisam ser commitados junto com a mudança. A CI cobre o esquecimento, mas só depois do push.
- O teste que exporta o contrato escreve fora da pasta do backend.
- As anotações do springdoc nos controllers passam a ser parte do contrato e precisam de revisão como código.
- O TypeScript fica preso à linha 5 enquanto o gerador não suportar a 7.

## Alternativas consideradas

- **Plugin Maven do springdoc, que sobe a aplicação e baixa o documento:** exige banco e as variáveis do emissor de tokens no build, e deixa o documento fora dos testes. O teste de integração reaproveita a infraestrutura que os testes já têm.
- **Contrato escrito à mão (contract-first) e código gerado a partir dele:** o design da API já cumpre o papel de desenho prévio, em prosa. Manter também um YAML à mão seria uma terceira fonte para sincronizar.
- **Tipos gerados a cada build, fora do Git:** o pacote não compilaria sem o backend ter rodado antes, e a pull request esconderia o impacto da mudança, pelo mesmo motivo que levou a versionar o código do jOOQ.
- **Casos de teste escritos separadamente em Java e em TypeScript:** é exatamente a divergência que a decisão quer evitar.
