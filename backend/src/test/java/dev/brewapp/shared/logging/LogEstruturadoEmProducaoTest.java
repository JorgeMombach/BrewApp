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

        String linhaDoEvento = capturedOutput.getOut().lines()
                .filter(linha -> linha.contains("Lote criado por"))
                .findFirst()
                .orElseThrow();
        assertThat(linhaDoEvento)
                .startsWith("{")
                .endsWith("}")
                .contains("\"log\":{\"level\":\"INFO\"")
                .contains("\"message\":\"Lote criado por f***@exemplo.com\"")
                .contains("\"correlationId\":\"rastro-json\"")
                .contains("\"service\":{\"name\":\"brewapp\"")
                .doesNotContain("fulano@exemplo.com");
    }
}
