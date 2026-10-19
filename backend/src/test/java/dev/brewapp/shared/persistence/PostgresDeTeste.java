package dev.brewapp.shared.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * PostgreSQL real para os testes de integração (spec 10), com as mesmas roles do ambiente local: o container roda
 * o init script do docker-compose, e o Flyway e a aplicação conectam cada um com a sua role (spec 9.3).
 * Um container só por JVM, compartilhado entre os contextos do Spring; o Ryuk o remove no fim.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresDeTeste {

    private static final String IMAGEM = "postgres:18.6";
    private static final String INIT_SCRIPT = "../infra/postgres/init/01-init.sh";
    private static final int PERMISSAO_DE_EXECUCAO = 0755;

    static final String ROLE_DE_MIGRACAO = "brewapp_owner";
    static final String ROLE_DA_APLICACAO = "brewapp_app";
    private static final String SENHA_DE_MIGRACAO = "senha-de-teste-owner";
    private static final String SENHA_DA_APLICACAO = "senha-de-teste-app";

    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(IMAGEM)
            .withDatabaseName("brewapp")
            .withEnv("BREWAPP_DB_OWNER_USER", ROLE_DE_MIGRACAO)
            .withEnv("BREWAPP_DB_OWNER_PASSWORD", SENHA_DE_MIGRACAO)
            .withEnv("BREWAPP_DB_APP_USER", ROLE_DA_APLICACAO)
            .withEnv("BREWAPP_DB_APP_PASSWORD", SENHA_DA_APLICACAO)
            // O init script também cria o banco do Keycloak; nos testes ele só fica vazio
            .withEnv("KC_DB_USERNAME", "keycloak")
            .withEnv("KC_DB_PASSWORD", "senha-de-teste-keycloak")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(INIT_SCRIPT, PERMISSAO_DE_EXECUCAO),
                    "/docker-entrypoint-initdb.d/01-init.sh");

    static {
        postgreSQLContainer.start();
    }

    @Bean
    DynamicPropertyRegistrar propriedadesDoBancoDeTeste() {
        return dynamicPropertyRegistry -> {
            dynamicPropertyRegistry.add("BREWAPP_DB_URL", postgreSQLContainer::getJdbcUrl);
            dynamicPropertyRegistry.add("BREWAPP_DB_OWNER_USER", () -> ROLE_DE_MIGRACAO);
            dynamicPropertyRegistry.add("BREWAPP_DB_OWNER_PASSWORD", () -> SENHA_DE_MIGRACAO);
            dynamicPropertyRegistry.add("BREWAPP_DB_APP_USER", () -> ROLE_DA_APLICACAO);
            dynamicPropertyRegistry.add("BREWAPP_DB_APP_PASSWORD", () -> SENHA_DA_APLICACAO);
        };
    }
}
