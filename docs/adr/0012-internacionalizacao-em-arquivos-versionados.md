# ADR 0012: Internacionalização em arquivos versionados, com migração planejada para o banco

- **Status:** aceito
- **Data:** 19/10/2026
- **História:** BREW-19 (item 12 da seção 11 da spec, escrito junto com a implementação)
- **Referências:** spec do MVP, seção 8; design da API, seções 1.6 e 1.11 e `GET /me`; ADR 0013; ADR 0017

## Contexto

O BrewApp é usado em português (Brasil), espanhol e inglês desde o início (spec, seção 8). O backend não traduz nada: devolve códigos (de erro, de status, de unidade) e o cliente mostra o texto no idioma do usuário. Três clientes vão exibir esses textos: o app web, o desktop (o mesmo app empacotado) e o mobile, que precisa funcionar sem rede.

Sem uma regra única, cada tela acaba com texto fixo, cada código de erro novo precisa de uma busca manual para saber se alguém traduziu, e um idioma fica para trás sem ninguém perceber. Também é preciso decidir como ler números digitados: a mesma densidade chega como `1,050` de quem usa vírgula e como `1.050` de quem usa ponto, e o espanhol do México usa ponto, diferente do da Espanha.

As traduções devem ir para o banco quando houver quem traduza sem ser desenvolvedor (spec, seção 12). A convenção de agora precisa servir para essa migração sem conversão.

## Decisão

**Os textos ficam em arquivos JSON versionados no `packages/shared`, um por idioma, com chaves planas e tipadas a partir do pt-BR. O app usa o i18next, e o compilador, o lint, a CI e um teste do backend garantem que nenhum texto fica de fora.**

1. **Arquivos.** `packages/shared/i18n/pt-BR.json`, `es.json` e `en.json`, embutidos no app no build (sem carregamento pela rede, o que resolve o offline). O pt-BR é a referência das chaves.
2. **Chaves planas.** Cada chave é um texto único (`"app.paginaInicial.titulo": "Diário de adega"`), sem aninhamento: o ponto faz parte do nome. No banco, cada chave vira uma linha `(chave, idioma, texto)`, sem conversão.
3. **Convenção de nomes.**

   | Grupo | Formato | Exemplo |
   |---|---|---|
   | Interface | `app.<tela>.<elemento>` (o módulo de negócio entra no lugar de `app` quando houver telas por módulo) | `app.layout.navegacaoPrincipal` |
   | Valor de domínio | `dominio.<enum>.<CODIGO>`, com o código como a API devolve | `dominio.unidade.PLATO` |
   | Erro da API ou do cliente | `erro.<CODIGO>` | `erro.ACESSO_NEGADO`, `erro.SEM_CONEXAO` |
   | Item de validação | `validacao.<CODIGO>` | `validacao.TAMANHO_INVALIDO` |
   | Auxiliares de cada grupo | camelCase, para não colidir com um código | `erro.desconhecido`, `erro.referencia` |

4. **O que leva chave.** Todo texto que o usuário vê ou que o leitor de tela lê: botões, labels, títulos, mensagens, `aria-label`, `title`, `alt`, `placeholder`, inclusive o nome do produto. Não levam chave: o conteúdo digitado pelo usuário, números e datas (formatados pelo `Intl`), mensagens técnicas (log, `title` do Problem Details, `message` do `ErroDaApi`), os símbolos de unidade e o nome de cada idioma no seletor, que aparece na própria língua (`Intl.DisplayNames`).
5. **Frases com variáveis, nunca concatenadas.** A ordem das palavras muda entre idiomas, por isso a frase inteira é uma chave com `{{variavel}}`. Números dentro de frases usam o formato do i18next (`{{value, number}}`), que segue o idioma. Plural usa as formas do i18next (`_one`, `_other` e, se preciso, `_many`), presentes nos três idiomas.
6. **Chaves tipadas.** O tipo das traduções vem do pt-BR, e o `es` e o `en` precisam ter todas as chaves dele. O `t()` do app só aceita chave existente (`CustomTypeOptions` do i18next, com `strictKeyChecks`). Funções como `chaveDaUnidade` montam a chave pelo código do catálogo, e uma unidade nova sem tradução quebra a compilação.
7. **Garantias além do compilador.**
   - **Lint:** o ESLint do app web barra texto solto no JSX e literal nos atributos lidos pelo usuário.
   - **CI:** o job de traduções compara os três idiomas com o pt-BR e falha com chave faltando, chave a mais, texto vazio ou variáveis diferentes.
   - **Backend:** um teste confere que todo código de erro do backend (genéricos, de validação e os enums `CodigoDeErro` de qualquer módulo) tem chave `erro.` ou `validacao.`.
8. **Idioma em uso.** A ordem é: a preferência `idioma` do `GET /me` (a partir do login, na BREW-6), depois a escolha manual guardada no navegador, depois as preferências do navegador (variantes caem na língua: `es-MX` vira `es`, `pt-PT` vira `pt-BR`) e, por fim, `pt-BR`. O `<html lang>` acompanha o idioma em uso.
9. **Erros.** A tela traduz o `ErroDaApi` pelo `code` e pelos `params` (ADR 0017). Um código sem tradução, de um backend mais novo que o app, mostra uma mensagem genérica, nunca o `title`. Quando há resposta, a tela mostra o `correlationId` como código para o suporte.
10. **Números e datas.** São exibidos pelo `Intl` no formato do idioma. A data aceita o fuso como parâmetro, para usar a preferência `fusoHorario` do `GET /me`. **Número digitado** aceita vírgula ou ponto como separador decimal, em qualquer idioma, e não aceita separador de milhar. Por isso o valor de um campo de entrada é preenchido sem separador de milhar.

## Consequências

**Positivas**

- Texto faltando é pego antes do merge, na compilação, no lint, na CI ou no teste do backend, e não pelo usuário.
- O mobile reaproveita os arquivos, os tipos, a escolha de idioma e a formatação do `packages/shared`.
- A migração para o banco não muda a convenção nem as telas.
- A pull request mostra o texto novo junto com o código que o usa.

**Negativas**

- Só quem tem acesso ao repositório edita as traduções, e uma correção de texto passa por pull request e por um novo build do app.
- Com o pt-BR como referência, toda chave nasce primeiro em português.
- A regra do número digitado lê `1.000` como 1, e não como mil. Na densidade isso não acontece, mas em volume e massa sim. Os campos de volume e massa precisam de atenção na primeira tela que os usar (Recipientes, BREW-7), por exemplo mostrando o valor interpretado ao lado do campo.
- Se as formas de plural do CLDR mudarem num idioma, como o `many` do português e do espanhol, as três traduções da chave mudam juntas.

## Migração para o banco

Quando houver quem traduza sem ser desenvolvedor, a migração segue dois passos:

1. **Banco como fonte, arquivo no build.** As traduções passam a ser editadas numa tela de administração. Um script exporta os mesmos três arquivos JSON antes do build, e o app continua embutindo as traduções, os tipos e as conferências da CI.
2. **Publicação em CDN**, só se for preciso corrigir texto sem novo build. Exige cache local, para o mobile continuar funcionando offline.

## Alternativas consideradas

- **Chaves aninhadas (`{ "app": { "paginaInicial": { "titulo": ... } } }`):** são o padrão do i18next, mas a migração para o banco precisaria achatar as chaves, e a busca pelo nome completo no código não acha o texto.
- **Mensagens no formato ICU, com FormatJS ou react-intl:** o plural e a seleção ficam mais expressivos, mas a sintaxe é mais pesada para quem traduz e a tipagem das chaves é mais fraca. O i18next tem tipagem das chaves e das variáveis sem gerador.
- **Traduções no banco desde já:** resolvem um problema que ainda não existe (tradutores que não são desenvolvedores) e colocam o banco no caminho do build e da CI.
- **Detecção de idioma pelo plugin do i18next:** o mapeamento das variantes regionais e a prioridade da preferência do usuário cabem numa função pequena e testada no pacote compartilhado, que o mobile também usa.
- **Número digitado seguindo à risca o separador do idioma:** quebraria a entrada de quem usa ponto com o idioma `es` ou `pt-BR`, e `1.050` viraria mil e cinquenta no pt-BR.
