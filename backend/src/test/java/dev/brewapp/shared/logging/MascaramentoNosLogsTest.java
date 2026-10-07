package dev.brewapp.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

/**
 * A PoliticaDeMascaramento está de fato ligada ao console pelo log4j2-spring.xml (perfil sem dev).
 * Contexto novo: a configuração do Log4j2 é global na JVM, e um contexto reaproveitado do cache ficaria com a do
 * último perfil que subiu (ex.: dev). As asserções valem para texto e para JSON: depois que um contexto prod sobe,
 * o Boot mantém o formato estruturado na propriedade de sistema até o fim da JVM de testes.
 */
@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Import(PostgresDeTeste.class)
class MascaramentoNosLogsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MascaramentoNosLogsTest.class);

    private static final String TOKEN = JwtFalsoDeTeste.gerar();

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void tokenEEmailNaoChegamAoConsole(CapturedOutput capturedOutput) {
        LOGGER.info("Requisição com Authorization: Bearer {} do usuário {}", TOKEN, "fulano@exemplo.com");

        assertThat(capturedOutput.getOut())
                .contains("Requisição com Authorization: Bearer *** do usuário f***@exemplo.com")
                .doesNotContain(TOKEN)
                .doesNotContain("fulano@exemplo.com");
    }

    @Test
    void valoresDoMdcTambemSaoMascarados(CapturedOutput capturedOutput) {
        // O console mostra o correlationId do MDC (texto ou JSON); um valor sensível ali também é mascarado
        MDC.put("correlationId", "senha=vazou");

        LOGGER.info("Mensagem sem dado sensível");

        assertThat(capturedOutput.getOut())
                .contains("senha=***")
                .doesNotContain("vazou");
    }

    @Test
    void emailNoLoggerDoJooqEhMascaradoForaDoDev(CapturedOutput capturedOutput) {
        LoggerFactory.getLogger("org.jooq.tools.LoggerListener").warn("select 'fulano@exemplo.com'");

        assertThat(capturedOutput.getOut()).contains("select 'f***@exemplo.com'");
    }
}
