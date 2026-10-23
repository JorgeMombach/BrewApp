# ADR 0010: Autenticação delegada a provedor OpenID Connect (Keycloak), com troca revisável

- **Status:** proposto
- **Data:** 22/10/2026
- **História:** BREW-23
- **Referências:** spec do MVP, seções 9.1 e 9.5; design da API, seção 1.2; ADR 0017

## Contexto

O BrewApp precisa reconhecer quem usa o sistema pela web, pelo desktop e, mais tarde, pelo celular. Guardar senhas traria para o projeto tudo o que vem junto com elas: hash, recuperação, bloqueio contra força bruta, MFA e o risco de vazamento. A infraestrutura de produção ainda não está definida, e o provedor de identidade definitivo depende dela (spec, seção 12).

Cada cliente tem uma restrição diferente:

- **Web.** Tudo o que fica no navegador pode ser lido por um script injetado. A spec manda guardar o token só em memória (seção 9.5).
- **Desktop.** É um app nativo (Tauri) que roda o mesmo código da web. Ele não pode guardar segredo de client, e o token precisa ficar no armazenamento seguro do sistema operacional. Um app nativo também não deve pedir a senha dentro da própria janela.
- **Sessão longa.** O token de acesso dura pouco. Renovar com o refresh token evita pedir senha a cada poucos minutos, mas um refresh token roubado não pode valer para sempre.

## Decisão

**O login é delegado a um provedor OpenID Connect (Keycloak no MVP); o backend só valida tokens, e cada cliente faz Authorization Code com PKCE, com renovação rotativa e o token de acesso apenas em memória.**

1. **O backend é só Resource Server.** Ele valida assinatura (JWKS), emissor, destinatário (`brewapp-api`) e validade de cada token, e responde 401 em Problem Details quando algo falha. Emissor e destinatário vêm de variáveis de ambiente, então trocar o Keycloak por outro provedor OIDC, como o Cognito, é configuração, não código.
2. **Um client público por plataforma, sempre com PKCE S256.** São três: `brewapp-web`, `brewapp-desktop` e `brewapp-mobile`. Nenhum tem segredo, fluxo implícito ou login por senha direto (*direct access grants*).
3. **Rotação do refresh token.** O realm revoga o refresh token a cada uso: cada um vale uma única vez. O token de acesso dura 5 minutos, a sessão ociosa 30 minutos e a sessão inteira 10 horas.
4. **Web: redirect e token só em memória.** O app vai ao Keycloak e volta por `/auth/callback`. O `state` e o `code_verifier` do PKCE ficam no `sessionStorage` só durante essa ida e volta, porque precisam sobreviver à troca de página. Tokens nunca vão para `localStorage`, `sessionStorage` ou cookie. Recarregar a página encerra a sessão local; o próximo login volta na hora pela sessão do Keycloak, sem pedir senha.
5. **Desktop: navegador do sistema com retorno por loopback (RFC 8252).** O app abre o login no navegador padrão e recebe o retorno num servidor local em `127.0.0.1`, numa porta livre a cada login. O servidor é do próprio projeto, na camada Rust do Tauri: aceita só `GET /auth/callback`, responde uma página sem script e desiste após 5 minutos. A conferência do `state` e a troca do code ficam com a biblioteca OIDC.
6. **Desktop: refresh token no cofre do sistema operacional.** Credential Manager no Windows, Keychain no macOS e Secret Service no Linux. O refresh token e o id token ficam em entradas separadas e gravados em UTF-8, porque o Credential Manager limita cada segredo a 2560 bytes. O token de acesso fica só em memória. Ao abrir, o app renova a sessão a partir do cofre.
7. **Renovação sob demanda e uma por vez.** O token é renovado quando alguém o pede a menos de 30 segundos da expiração. Chamadas simultâneas esperam a mesma renovação, porque duas renovações em paralelo, com rotação, derrubariam a sessão. Recusa do provedor encerra a sessão local; falha de rede mantém a sessão e chega à tela como `SEM_CONEXAO` (ADR 0017).
8. **Logout encerra a sessão no provedor.** A sessão local é limpa primeiro, para o usuário sair mesmo sem rede. A web passa pelo logout do Keycloak (RP-Initiated Logout, com `id_token_hint`). O desktop chama o endpoint de logout por `POST` com o refresh token, sem abrir o navegador.
9. **Biblioteca `oidc-client-ts`, na camada de baixo nível.** O app usa o `OidcClient` para montar o pedido de login, processar o retorno e renovar, com uma classe própria por cima que guarda a sessão e decide quando renovar. Web e desktop compartilham essa classe e só diferem em como fazem o login e onde guardam o refresh token.

## Consequências

**Positivas**

- O sistema não guarda nenhuma senha. Recuperação de senha, MFA, bloqueio contra força bruta e login social vêm do provedor.
- Um refresh token vazado vale uma vez: o primeiro uso pelo dono ou pelo atacante invalida o outro.
- No desktop, o usuário vê o endereço real do provedor e usa o gerenciador de senhas do navegador.
- A lógica de sessão é a mesma nas duas plataformas e é testada sem navegador nem Keycloak.

**Negativas**

- Na web, recarregar a página exige passar de novo pelo Keycloak. Isso só some com o padrão BFF, previsto para depois do MVP (spec, seção 9.5).
- O desktop depende de um navegador instalado, e o usuário precisa voltar à janela do app depois do login.
- O servidor de loopback é código do projeto para manter. Qualquer processo local pode chamá-lo, mas um retorno sem o `state` gerado pelo app é recusado.
- Com a sessão ociosa de 30 minutos, quem deixa o desktop parado mais que isso precisa entrar de novo. Sessões mais longas no desktop, com `offline_access`, ficam para avaliar com o uso.
- A abertura do login no desktop está restrita ao Keycloak local, assim como a CSP. No primeiro deploy, os dois ganham o endereço real.
- No macOS e no Linux, a origem do app empacotado é `tauri://localhost`, que ainda não está liberada no realm nem no CORS do backend. Só o Windows foi testado.
- O `oidc-client-ts` declara o tipo da renovação (`RefreshState`), mas não o exporta em tempo de execução na versão 3.5. O app monta um objeto com o mesmo formato; uma mudança na biblioteca pode exigir ajuste.

## Alternativas consideradas

- **Login próprio no backend (Spring Authorization Server ou tabela de usuários):** traria a guarda de senhas e tudo o que vem com ela, sem ganho para o MVP.
- **Cognito ou outro provedor gerenciado desde já:** depende da infraestrutura de produção, ainda indefinida. A validação só pelo padrão OIDC deixa a troca aberta.
- **Token no `localStorage` na web:** sobrevive ao recarregar, mas fica ao alcance de qualquer script injetado. A spec veta.
- **BFF na web já no MVP:** resolveria o recarregar com cookie protegido, mas exige um componente de servidor a mais. Fica para depois do MVP.
- **Login dentro da janela do desktop (webview):** menos código, mas contraria a RFC 8252, esconde o endereço do provedor e não usa o gerenciador de senhas do navegador.
- **`tauri-plugin-oauth` para o loopback:** confia num cabeçalho enviado por um script da página de retorno, não confere o caminho e escuta sem prazo. O servidor próprio tem poucas linhas e só biblioteca padrão.
- **Stronghold do Tauri no desktop:** é um arquivo cifrado com senha do próprio app, não o cofre do sistema operacional que a spec pede.
- **`UserManager` do `oidc-client-ts` na web:** resolve renovação e armazenamento sozinho, mas só para a web. Com o `OidcClient`, as duas plataformas compartilham a mesma lógica de sessão.
