package dev.brewapp.shared.persistence;

import static dev.brewapp.shared.persistence.PostgresDeTeste.ROLE_DA_APLICACAO;
import static dev.brewapp.shared.persistence.PostgresDeTeste.ROLE_DE_MIGRACAO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Separação de roles do banco (spec 9.3): o Flyway migra com a role dona dos schemas, e a aplicação conecta com
 * uma role que só manipula dados. O dono de uma tabela ignora o RLS, por isso a aplicação nunca pode sê-lo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresDeTeste.class)
class RolesDoBancoTest {

    private static final String PRIVILEGIO_INSUFICIENTE = "42501";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    void flywayAplicaAsMigrationsComARoleDeMigracao() {
        var instaladoPor = comoMigracao()
                .sql("SELECT DISTINCT installed_by FROM flyway.flyway_schema_history")
                .query(String.class)
                .list();

        assertThat(instaladoPor).containsExactly(ROLE_DE_MIGRACAO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"flyway", "organizacao", "recipiente", "lote", "adega"})
    void schemasPertencemARoleDeMigracao(String schema) {
        var dono = comoMigracao()
                .sql("SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname = ?")
                .param(schema)
                .query(String.class)
                .single();

        assertThat(dono).isEqualTo(ROLE_DE_MIGRACAO);
    }

    @Test
    void aplicacaoConectaComRoleSemPrivilegiosEspeciais() {
        Map<String, Object> roleDaConexao = comoAplicacao()
                .sql("""
                        SELECT rolname, rolsuper, rolcreaterole, rolcreatedb, rolbypassrls
                        FROM pg_roles
                        WHERE rolname = current_user
                        """)
                .query()
                .singleRow();

        assertThat(roleDaConexao)
                .containsEntry("rolname", ROLE_DA_APLICACAO)
                .containsEntry("rolsuper", false)
                .containsEntry("rolcreaterole", false)
                .containsEntry("rolcreatedb", false)
                .containsEntry("rolbypassrls", false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"organizacao", "recipiente", "lote", "adega", "public"})
    void aplicacaoNaoCriaTabelas(String schema) {
        assertNegadoPorPrivilegio(() -> comoAplicacao()
                .sql("CREATE TABLE " + schema + ".tabela_indevida (id integer)")
                .update());
    }

    @Test
    void aplicacaoNaoCriaSchemas() {
        assertNegadoPorPrivilegio(() -> comoAplicacao()
                .sql("CREATE SCHEMA schema_indevido")
                .update());
    }

    /** Tabela criada pela role de migração, como faria uma migration: vale o ALTER DEFAULT PRIVILEGES do schema. */
    @Nested
    class TabelaDeModulo {

        private static final String TABELA = "lote.tabela_de_teste_de_roles";

        @BeforeEach
        void criarTabelaComoMigracao() {
            comoMigracao().sql("CREATE TABLE " + TABELA + " (id integer PRIMARY KEY, descricao text)").update();
            comoMigracao().sql("INSERT INTO " + TABELA + " VALUES (1, 'original')").update();
        }

        @AfterEach
        void removerTabela() {
            comoMigracao().sql("DROP TABLE " + TABELA).update();
        }

        @Test
        void aplicacaoLeInsereEAltera() {
            var aplicacao = comoAplicacao();

            aplicacao.sql("INSERT INTO " + TABELA + " VALUES (2, 'inserido')").update();
            aplicacao.sql("UPDATE " + TABELA + " SET descricao = 'alterado' WHERE id = 1").update();

            var descricoes = aplicacao.sql("SELECT descricao FROM " + TABELA + " ORDER BY id")
                    .query(String.class)
                    .list();
            assertThat(descricoes).containsExactly("alterado", "inserido");
        }

        /** Exclusão física é exceção (spec 5.5): DELETE é concedido tabela a tabela, na migration que precisar. */
        @Test
        void aplicacaoNaoExcluiLinhasSemGrantExplicito() {
            assertNegadoPorPrivilegio(() -> comoAplicacao().sql("DELETE FROM " + TABELA).update());
        }

        @Test
        void aplicacaoNaoAlteraNemRemoveATabela() {
            assertNegadoPorPrivilegio(() -> comoAplicacao()
                    .sql("ALTER TABLE " + TABELA + " ADD COLUMN coluna_indevida text")
                    .update());
            assertNegadoPorPrivilegio(() -> comoAplicacao().sql("DROP TABLE " + TABELA).update());
        }
    }

    private JdbcClient comoAplicacao() {
        return JdbcClient.create(dataSource);
    }

    /** Conexão avulsa com a role de migração, a mesma que o Flyway usa (spring.flyway.user). */
    private JdbcClient comoMigracao() {
        return JdbcClient.create(new DriverManagerDataSource(
                environment.getRequiredProperty("BREWAPP_DB_URL"),
                environment.getRequiredProperty("BREWAPP_DB_OWNER_USER"),
                environment.getRequiredProperty("BREWAPP_DB_OWNER_PASSWORD")));
    }

    private static void assertNegadoPorPrivilegio(ThrowingCallable comandoSql) {
        assertThatThrownBy(comandoSql)
                .rootCause()
                .asInstanceOf(type(SQLException.class))
                .extracting(SQLException::getSQLState)
                .isEqualTo(PRIVILEGIO_INSUFICIENTE);
    }
}
