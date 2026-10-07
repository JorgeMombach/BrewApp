# ADR 0014: Log4j2 por trás da fachada SLF4J, com política de log de SQL por ambiente

- **Status:** aceito
- **Data:** 07/10/2026
- **História:** BREW-16
- **Referências:** spec do MVP, seção 7; design da API, seções 1.2 e 1.6; Definition of Done, critério geral 4

## Contexto

O BrewApp precisa de logs que ajudem a investigar problemas de comportamento e de desempenho sem expor dados sensíveis. Duas necessidades puxam em direções opostas. Em desenvolvimento, quem investiga quer ver o SQL exatamente como foi executado, com os valores, para copiar e rodar no banco. Em produção, esses mesmos valores são dados de clientes e não podem ir para o log.

Além disso, o aplicativo do celular sincroniza eventos registrados offline. Quando uma sincronização falha, é preciso juntar o que o celular enviou com o que o servidor registrou. Sem um identificador comum, isso vira trabalho de comparar horários.

O Spring Boot usa o Logback por padrão. A escolha da implementação de log é um detalhe, mas detalhes assim tendem a vazar para o código quando ninguém os protege.

## Decisão

1. **O código loga só pela fachada SLF4J, e o Log4j2 é a implementação.** O Logback é excluído do projeto e proibido no build: se algum starter novo o trouxer de volta, o build falha. Uma regra de arquitetura impede o uso direto da API do Log4j2, com uma única exceção, a infraestrutura de log, que precisa implementar plugins do próprio Log4j2.
2. **A política de SQL muda por ambiente.** Em desenvolvimento, o listener nativo do jOOQ registra todo SQL com os valores preenchidos. Em produção, um listener próprio registra só as consultas que passam de um limite de tempo configurável (500 ms no início), com a duração e o SQL com marcadores no lugar dos valores. A duração vai do início da execução até o fim da leitura do resultado.
3. **O driver do PostgreSQL não inclui o detalhe do erro nas exceções em produção.** É no detalhe que o banco coloca os valores dos registros, como o e-mail que violou uma chave única.
4. **Produção loga em JSON no formato Elastic Common Schema.** O formato é suportado nativamente pelo Spring Boot e entendido pelas ferramentas de log mais comuns, sem amarrar o projeto a nenhuma delas. Em desenvolvimento, o log continua em texto.
5. **Toda requisição tem um identificador de correlação.** Ele é aceito do cliente, no cabeçalho `X-Correlation-Id`, desde que siga um formato restrito; fora disso, ou quando não vem, o servidor gera um. O identificador é definido antes da camada de segurança, entra em toda linha de log da requisição e volta na resposta, no cabeçalho e no corpo dos erros. O formato restrito existe porque o valor vem de fora e vai para o log.
6. **Todo evento de log passa por um mascaramento antes de ser escrito.** Tokens, credenciais e segredos são sempre ocultados, e e-mails ficam só com a primeira letra e o domínio. A única exceção é o SQL registrado em desenvolvimento, que mantém o e-mail para continuar executável, mas ainda oculta os tokens.

## Consequências

**Positivas**

- Trocar a implementação de log não afeta nenhuma classe, e a regra de arquitetura garante que continue assim.
- Em desenvolvimento, uma consulta suspeita pode ser copiada do log e executada direto no banco.
- Em produção, o log de consultas lentas aponta problemas de desempenho sem expor dados.
- Um problema relatado por um usuário pode ser rastreado pelo identificador que aparece na resposta de erro, inclusive quando a requisição vem de uma sincronização do celular.
- O mascaramento protege contra o descuido mais comum: logar um objeto ou cabeçalho que leva token ou e-mail.

**Negativas**

- O mascaramento é a segunda linha de defesa, não a primeira. Ele não alcança o stack trace das exceções e não reconhece dados pessoais em texto livre, como nomes. A regra continua sendo não colocar dado pessoal em mensagens de log nem de exceção.
- Ocultar o detalhe dos erros do PostgreSQL deixa a mensagem principal intacta, e algumas mensagens principais também trazem o valor recebido (por exemplo, um texto inválido para um campo numérico).
- Valores escritos diretamente no texto do SQL (inline), em vez de passados como parâmetros, aparecem no log de consultas lentas. Dados do usuário nunca devem ser escritos dessa forma.
- O projeto mantém um arquivo de configuração do Log4j2 próprio, derivado do padrão do Spring Boot, que precisa ser revisto quando o Spring Boot mudar o dele.
- Desde o Java 23, o processamento de anotações não é implícito, e o processador do Log4j2 precisa estar declarado no build para o plugin de mascaramento ser encontrado.
- Nos testes, a configuração do Log4j2 é global na JVM e sobrevive entre contextos do Spring. Testes que verificam o formato ou o mascaramento precisam de um contexto próprio.

## Alternativas consideradas

- **Manter o Logback, que é o padrão do Spring Boot:** funcionaria, mas a spec já tinha definido o Log4j2, e a fachada SLF4J deixa essa escolha reversível.
- **Mascarar com substituição de texto direto no padrão do log:** só vale para o formato texto. O JSON de produção não passa pelo padrão, então o mascaramento ficaria justamente onde menos importa.
- **Proxy de log do driver JDBC (como datasource-proxy ou P6Spy) para o SQL:** adicionaria uma dependência para algo que o jOOQ já oferece com listeners.
- **Rastreamento distribuído (Micrometer Tracing) no lugar do identificador próprio:** resolve um problema maior que o atual, de um único serviço. Pode ser adotado quando houver a primeira extração de serviço. Nesse momento, o identificador de correlação pode virar um atributo do rastro.
