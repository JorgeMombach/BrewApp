# ADR 0017: Cliente HTTP tipado pelo contrato, com toda falha entregue como ErroDaApi

- **Status:** aceito
- **Data:** 16/10/2026
- **História:** BREW-17
- **Referências:** spec do MVP, seções 4.5, 8 e 9.5; design da API, seções 1.2 e 1.6; ADR 0013

## Contexto

O ADR 0013 garante que os tipos TypeScript acompanham o contrato da API. Faltava decidir como os aplicativos usam esses tipos para chamar o backend e, principalmente, como uma falha chega às telas.

Uma chamada pode falhar de jeitos bem diferentes:

- o backend responde com um Problem Details, com `code`, `params` e `correlationId` (design da API, seção 1.6);
- a resposta de erro não segue esse formato, como a página HTML de um proxy ou de um gateway;
- a requisição nem chega ao servidor: sem rede, servidor fora do ar ou bloqueio de CORS;
- a própria tela cancela a requisição, por exemplo ao trocar de página.

Se cada tela tratasse esses casos à sua maneira, algumas exibiriam o `title` (que é só para log), outras perderiam o `correlationId` que o usuário precisa informar ao relatar um problema, e uma resposta fora do formato seria lida como se fosse Problem Details. A internacionalização (spec, seção 8) também depende de toda falha ter um código que a interface saiba traduzir.

## Decisão

**O app web chama a API por um cliente gerado a partir dos tipos do contrato, e toda falha chega às telas como um único tipo de erro, o `ErroDaApi`, sempre com um código traduzível.**

1. **Cliente tipado pelo contrato.** O cliente usa o `openapi-fetch`, do mesmo autor do `openapi-typescript` (ADR 0013), alimentado pelos tipos gerados: caminho, parâmetros, corpo e resposta de cada endpoint são conferidos pelo compilador. Não há código gerado além dos tipos, e a biblioteca é uma camada fina sobre o `fetch`.
2. **Uma única porta de saída para falhas.** Toda resposta de erro e toda falha de rede rejeitam a chamada com `ErroDaApi`. Quem chama não precisa conferir, a cada resposta, se veio dado ou erro: se a chamada termina sem erro, os dados estão lá.
3. **O formato é conferido antes de ser confiado.** O corpo de uma resposta de erro só é lido como Problem Details depois de uma checagem em tempo de execução, que fica no pacote compartilhado para servir também ao mobile. Corpo fora do formato vira `RESPOSTA_INESPERADA`, com o status da resposta e o `correlationId` do cabeçalho, quando houver.
4. **Falha sem resposta também tem código.** Requisição que não chega ao servidor vira `SEM_CONEXAO`, sem status e sem `correlationId`, guardando a falha original como causa para o log. Os dois códigos do cliente estão na seção 1.6 do design da API, marcados como códigos que o backend nunca devolve.
5. **Cancelamento não é falha.** Uma requisição cancelada pela própria tela segue com o erro original de cancelamento, para não ser exibida como problema de conexão.
6. **Cabeçalhos do contrato aplicados num só lugar.** O token de acesso e a organização em uso (`X-Organization-Id`) são lidos de funções fornecidas a quem cria o cliente e enviados em toda requisição quando existem. O cliente não guarda token: na web, ele fica em memória (spec, seção 9.5), e quem o mantém é o fluxo de login.

## Consequências

**Positivas**

- Toda tela trata erro do mesmo jeito: decide pelo `code`, exibe a tradução e mostra o `correlationId` quando houver.
- Um endpoint renomeado ou com corpo diferente quebra a compilação do app, e não a tela em uso.
- A regra de "nunca exibir o `title`" fica fácil de seguir: a mensagem do `ErroDaApi` é técnica e serve só para log.
- O mobile pode reaproveitar a checagem do Problem Details e os cabeçalhos do pacote compartilhado.

**Negativas**

- O `openapi-fetch` ainda está na versão 0.x, e uma versão nova pode mudar a API. O uso está concentrado em um arquivo, o que limita o custo de uma troca.
- Rejeitar toda resposta de erro obriga quem chama a usar tratamento de exceção mesmo para erros esperados, como um 404 de recurso inexistente.
- Os dois códigos do cliente precisam de tradução como qualquer código da API, e ficam fora do teste de contrato dos códigos de erro, que só cobre o backend.

## Alternativas consideradas

- **`fetch` puro com funções escritas à mão por endpoint:** perde a conferência de tipos do caminho e dos parâmetros, que é o motivo de gerar os tipos (ADR 0013).
- **Gerador de cliente completo (como o `openapi-generator`):** gera classes e funções por endpoint, mais código para revisar e versionar, sem ganho sobre os tipos que já existem.
- **Devolver dado ou erro na resposta, sem rejeitar (o padrão do `openapi-fetch`):** cada chamada precisaria conferir os dois casos, e uma tela que esquecesse a conferência seguiria com dado vazio em silêncio.
- **Axios com interceptadores:** resolve a centralização dos erros, mas não usa os tipos do contrato sem uma camada extra, e acrescenta uma dependência que o `fetch` nativo dispensa.
