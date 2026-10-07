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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

/**
 * Configuração real do application-dev.yml: todo SQL logado com os valores, sem o listener de consultas lentas.
 * Contexto novo: a configuração do Log4j2 é global na JVM e um contexto reaproveitado do cache não a refaz.
 */
@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ActiveProfiles({"test", "dev"})
@ExtendWith(OutputCaptureExtension.class)
@Import(PostgresDeTeste.class)
class LogDeSqlEmDesenvolvimentoTest {

    @Autowired
    private DSLContext dslContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void todoSqlEhLogadoComOsValoresPreenchidos(CapturedOutput capturedOutput) {
        dslContext.select(DSL.field("length({0}::text)", DSL.val("valor-visivel"))).fetch();

        assertThat(capturedOutput.getOut()).contains("'valor-visivel'");
    }

    /** Decisão da BREW-16: no dev, o e-mail fica visível só no SQL do jOOQ; tokens são mascarados mesmo ali. */
    @Test
    void sqlDoJooqMantemOEmailMasMascaraTokens(CapturedOutput capturedOutput) {
        dslContext.select(
                        DSL.field("length({0}::text)", DSL.val("fulano@exemplo.com")),
                        DSL.field("length({0}::text)", DSL.val("Bearer abc.def")))
                .fetch();

        assertThat(capturedOutput.getOut())
                .contains("'fulano@exemplo.com'")
                .contains("'Bearer ***'")
                .doesNotContain("abc.def");
    }

    @Test
    void foraDoJooqOEmailEhMascaradoTambemNoDev(CapturedOutput capturedOutput) {
        LoggerFactory.getLogger(LogDeSqlEmDesenvolvimentoTest.class).info("usuário fulano@exemplo.com");

        assertThat(capturedOutput.getOut())
                .contains("usuário f***@exemplo.com")
                .doesNotContain("fulano@exemplo.com");
    }

    @Test
    void listenerDeConsultasLentasNaoEhRegistrado() {
        assertThat(applicationContext.getBeansOfType(ConfiguracaoDeConsultasLentas.class)).isEmpty();
    }

    /** Contraprova do teste de produção: sem logServerErrorDetail=false, o Detail chega na mensagem. */
    @Test
    void excecaoDoBancoTrazODetail() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.execute(LogDeSqlEmProducaoTest.blocoQueFalhaComDetail()))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("fulano@exemplo.com");
        }
    }
}
