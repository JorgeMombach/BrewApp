package dev.brewapp.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
 * O mascaramento está de fato ligado ao console de texto pelo log4j2-spring.xml (perfil sem dev): mensagem e MDC pela
 * PoliticaDeMascaramento, exceção pelo %xwExMascarado. O JSON é coberto pelo LogEstruturadoEmProducaoTest.
 * Contexto novo: a configuração do Log4j2 é global na JVM, e um contexto reaproveitado do cache ficaria com a do
 * último perfil que subiu (ex.: dev).
 */
@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Import(PostgresDeTeste.class)
class MascaramentoNosLogsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MascaramentoNosLogsTest.class);

    private static final String TOKEN = JwtFalsoDeTeste.gerar();

    /**
     * Depois que um contexto prod sobe, o Boot deixa o formato JSON nesta propriedade de sistema até o fim da JVM de
     * testes. Limpa antes de o contexto deste teste subir, para o console voltar ao texto.
     */
    @BeforeAll
    static void voltarOConsoleParaTexto() {
        System.clearProperty("CONSOLE_LOG_STRUCTURED_FORMAT");
    }

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

    @Test
    void excecaoComSegredoSaiMascarada(CapturedOutput capturedOutput) {
        LOGGER.error("Erro inesperado", new IllegalStateException("select * from adega.evento where senha = 'segredo'"));

        assertThat(capturedOutput.getOut())
                .contains("java.lang.IllegalStateException: select * from adega.evento where senha = '***'")
                .doesNotContain("segredo");
        assertThat(linhaQueContem(capturedOutput, "Erro inesperado")).doesNotStartWith("{");
    }

    @Test
    void excecaoComEmailSaiMascarada(CapturedOutput capturedOutput) {
        LOGGER.error("Falha no cadastro", new IllegalStateException("Key (email)=(fulano@exemplo.com) already exists"));

        assertThat(capturedOutput.getOut())
                .contains("java.lang.IllegalStateException: Key (email)=(f***@exemplo.com) already exists")
                .doesNotContain("fulano@exemplo.com");
    }

    @Test
    void causaEncadeadaComDadoSensivelSaiMascarada(CapturedOutput capturedOutput) {
        IllegalArgumentException causa = new IllegalArgumentException("usuário fulano@exemplo.com com senha=abc");

        LOGGER.error("Falha ao salvar", new RuntimeException("Erro no repositório", causa));

        assertThat(linhaQueContem(capturedOutput, "Caused by:"))
                .isEqualTo("Caused by: java.lang.IllegalArgumentException: usuário f***@exemplo.com com senha=***");
        assertThat(capturedOutput.getOut())
                .doesNotContain("fulano@exemplo.com")
                .doesNotContain("senha=abc");
    }

    private static String linhaQueContem(CapturedOutput capturedOutput, String trecho) {
        return capturedOutput.getOut().lines()
                .filter(linha -> linha.contains(trecho))
                .findFirst()
                .orElseThrow();
    }
}
