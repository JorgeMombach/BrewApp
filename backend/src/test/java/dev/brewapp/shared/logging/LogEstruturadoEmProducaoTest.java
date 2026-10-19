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
 * Configuração real do application-prod.yml: cada evento é uma linha JSON (ECS), já mascarada, com o MDC.
 * Contexto novo: a configuração do Log4j2 é global na JVM e um contexto reaproveitado do cache não a refaz.
 */
@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ActiveProfiles({"test", "prod"})
@ExtendWith(OutputCaptureExtension.class)
@Import(PostgresDeTeste.class)
class LogEstruturadoEmProducaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogEstruturadoEmProducaoTest.class);

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void eventoSaiEmJsonEcsMascaradoEComOCorrelationId(CapturedOutput capturedOutput) {
        MDC.put("correlationId", "rastro-json");

        LOGGER.info("Lote criado por fulano@exemplo.com");

        String linhaDoEvento = linhaQueContem(capturedOutput, "Lote criado por");
        assertThat(linhaDoEvento)
                .startsWith("{")
                .endsWith("}")
                .contains("\"log\":{\"level\":\"INFO\"")
                .contains("\"message\":\"Lote criado por f***@exemplo.com\"")
                .contains("\"correlationId\":\"rastro-json\"")
                .contains("\"service\":{\"name\":\"brewapp\"")
                .doesNotContain("fulano@exemplo.com");
    }

    /** O tipo continua o da exceção original: a máscara age só na mensagem e no stack trace (MascaramentoDaExcecaoEmJson). */
    @Test
    void excecaoComSegredoSaiMascaradaNoErrorDoEcs(CapturedOutput capturedOutput) {
        LOGGER.error(
                "Erro inesperado", new IllegalStateException("select * from adega.evento where senha = 'segredo'"));

        assertThat(linhaQueContem(capturedOutput, "Erro inesperado"))
                .startsWith("{")
                .contains("\"error\":{\"type\":\"java.lang.IllegalStateException\"")
                .contains("\"message\":\"select * from adega.evento where senha = '***'\"")
                .contains(
                        "\"stack_trace\":\"java.lang.IllegalStateException: select * from adega.evento where senha = '***'")
                .doesNotContain("segredo");
    }

    @Test
    void excecaoComEmailSaiMascaradaNoErrorDoEcs(CapturedOutput capturedOutput) {
        LOGGER.error("Falha no cadastro", new IllegalStateException("Key (email)=(fulano@exemplo.com) already exists"));

        assertThat(linhaQueContem(capturedOutput, "Falha no cadastro"))
                .contains("\"message\":\"Key (email)=(f***@exemplo.com) already exists\"")
                .doesNotContain("fulano@exemplo.com");
    }

    @Test
    void causaEncadeadaComDadoSensivelSaiMascaradaNoStackTrace(CapturedOutput capturedOutput) {
        IllegalArgumentException causa = new IllegalArgumentException("usuário fulano@exemplo.com com senha=abc");

        LOGGER.error("Falha ao salvar", new RuntimeException("Erro no repositório", causa));

        assertThat(linhaQueContem(capturedOutput, "Falha ao salvar"))
                .contains("Caused by: java.lang.IllegalArgumentException: usuário f***@exemplo.com com senha=***")
                .doesNotContain("fulano@exemplo.com")
                .doesNotContain("senha=abc");
    }

    private static String linhaQueContem(CapturedOutput capturedOutput, String trecho) {
        return capturedOutput
                .getOut()
                .lines()
                .filter(linha -> linha.contains(trecho))
                .findFirst()
                .orElseThrow();
    }
}
