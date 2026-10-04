# Infraestrutura local

Sobe o PostgreSQL e o Keycloak que o BrewApp usa em desenvolvimento. Tudo roda em containers, então não é preciso instalar nenhum dos dois na máquina.

## Antes da primeira subida

É preciso ter o Docker Desktop (ou outro Docker com Compose v2) rodando.

Na raiz do repositório, copie o `.env.example` para `.env` e troque todos os valores marcados com `troque-me`. O `.env` fica fora do Git. A senha do usuário de teste precisa ter pelo menos 12 caracteres e um caractere especial, que é a política de senha do realm.

## Subir e parar

Os comandos rodam a partir desta pasta (`infra/`):

```bash
docker compose up -d --wait
```

O `--wait` só devolve o terminal quando os dois serviços estão saudáveis. A primeira subida demora mais, cerca de um minuto, porque o Keycloak cria as tabelas dele e importa o realm.

Para parar sem perder nada:

```bash
docker compose down
```

## O que fica disponível

| Serviço | Endereço | Acesso |
|---|---|---|
| PostgreSQL | `localhost:5433`, banco `brewapp` | roles `brewapp_owner` (migrations) e `brewapp_app` (aplicação), senhas no `.env` |
| Keycloak | `http://localhost:8180` | console de administração com o admin do `.env` |
| Realm `brewapp` | `http://localhost:8180/realms/brewapp` | usuário de teste do `.env` |

O PostgreSQL usa a porta 5433, e não a 5432 padrão, para não conflitar com uma instalação local. Os dois serviços só aceitam conexões da própria máquina.

O realm já vem com os clients da web, do desktop e do mobile, todos com PKCE obrigatório. Os tokens saem com o emissor `http://localhost:8180/realms/brewapp` e o destinatário `brewapp-api`, que são os valores que o backend valida.

## Resetar o ambiente

Apaga os containers e os volumes, ou seja, todos os dados do banco e do Keycloak, e sobe tudo do zero:

```bash
docker compose down -v
docker compose up -d --wait
```

O reset é necessário sempre que mudar algo que só é aplicado na primeira subida:

- o arquivo do realm em `keycloak/import/` (se o realm já existe, o Keycloak ignora o import);
- o script de inicialização do banco em `postgres/init/`;
- as senhas das roles do banco, do admin do Keycloak ou do usuário de teste no `.env`.

## Limitações conhecidas

- **Celular físico não alcança o ambiente.** Os serviços só escutam na própria máquina, e o emissor dos tokens é `localhost`, que dentro do celular aponta para o próprio celular. O emulador Android enxerga a máquina pelo endereço `10.0.2.2`, mas o emissor continua sendo um problema. Isso será resolvido junto com o aplicativo mobile.
- **Keycloak em modo de desenvolvimento.** Ele roda sem HTTPS e com configurações próprias para uso local. Não serve como base para produção.
