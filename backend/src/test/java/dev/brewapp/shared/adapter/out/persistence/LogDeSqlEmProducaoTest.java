package dev.brewapp.shared.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

/**
 * Configuração real do application-prod.yml: limite de 500 ms e driver sem o "Detail" do Postgres.
 * Contexto novo: a configuração do Log4j2 é global na JVM e um contexto reaproveitado do cache não a refaz.
 */
@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ActiveProfiles({"test", "prod"})
@ExtendWith(OutputCaptureExtension.class)
@Import(PostgresDeTeste.class)
class LogDeSqlEmProducaoTest {

    private static final String VALOR_SENSIVEL = "fulano@exemplo.com";

    @Autowired
    private DSLContext dslContext;

    @Autowired
    private DataSource dataSource;

    @Test
    void consultaLentaEhLogadaComOTempoESemOsValores(CapturedOutput capturedOutput) {
        dslContext.select(
                        DSL.field("pg_sleep({0}::float8)", DSL.val(0.6)),
                        DSL.field("length({0}::text)", DSL.val(VALOR_SENSIVEL)))
                .fetch();

        assertThat(capturedOutput.getOut())
                .containsPattern("Consulta lenta: \\d+ ms \\(limite 500 ms\\)")
                .contains("pg_sleep(")
                .doesNotContain(VALOR_SENSIVEL);
    }

    @Test
    void consultaRapidaNaoEhLogada(CapturedOutput capturedOutput) {
        dslContext.select(DSL.field("length({0}::text)", DSL.val(VALOR_SENSIVEL))).fetch();

        assertThat(capturedOutput.getOut())
                .doesNotContain("Consulta lenta")
                .doesNotContain(VALOR_SENSIVEL);
    }

    @Test
    void excecaoDoBancoNaoTrazODetailComValores() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.execute(blocoQueFalhaComDetail()))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("falha de teste")
                    .hasMessageNotContaining(VALOR_SENSIVEL);
        }
    }

    /** Mesmo formato de um erro de chave única: o valor do registro só aparece no Detail. */
    static String blocoQueFalhaComDetail() {
        return "DO $$ BEGIN RAISE EXCEPTION 'falha de teste' USING DETAIL = '" + VALOR_SENSIVEL + "'; END $$";
    }
}
