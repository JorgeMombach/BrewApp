package dev.brewapp.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

class ConversorDeExcecaoMascaradaTest {

    private static final String LOGGER_DO_JOOQ = "org.jooq.tools.LoggerListener";
    private static final String LOGGER_DA_APLICACAO = "dev.brewapp.qualquer.Classe";

    private final ConversorDeExcecaoMascarada conversorDeExcecaoMascarada =
            ConversorDeExcecaoMascarada.newInstance(new DefaultConfiguration(), null);

    @Test
    void mascaraSegredoNaMensagemDaExcecao() {
        IllegalStateException illegalStateException =
                new IllegalStateException("select * from adega.evento where senha = 'segredo'");

        String excecaoRenderizada = renderizar(evento(LOGGER_DA_APLICACAO, illegalStateException));

        assertThat(excecaoRenderizada)
                .contains("java.lang.IllegalStateException: select * from adega.evento where senha = '***'")
                .doesNotContain("segredo");
    }

    @Test
    void mascaraEmailNaMensagemDaExcecaoMesmoNoLoggerComEmailVisivel() {
        RuntimeException runtimeException = new RuntimeException("Key (email)=(fulano@exemplo.com) already exists");

        String excecaoRenderizada = renderizar(evento(LOGGER_DO_JOOQ, runtimeException));

        assertThat(excecaoRenderizada)
                .contains("Key (email)=(f***@exemplo.com) already exists")
                .doesNotContain("fulano@exemplo.com");
    }

    @Test
    void mascaraCausasEncadeadasEExcecoesSuprimidas() {
        IllegalStateException causaRaiz = new IllegalStateException("usuário fulano@exemplo.com");
        IllegalArgumentException causaIntermediaria = new IllegalArgumentException("senha=abc", causaRaiz);
        RuntimeException runtimeException = new RuntimeException("Falha ao salvar", causaIntermediaria);
        runtimeException.addSuppressed(new IllegalStateException("Authorization: Bearer abc.def"));

        String excecaoRenderizada = renderizar(evento(LOGGER_DA_APLICACAO, runtimeException));

        assertThat(excecaoRenderizada)
                .contains("java.lang.RuntimeException: Falha ao salvar")
                .contains("Caused by: java.lang.IllegalArgumentException: senha=***")
                .contains("Caused by: java.lang.IllegalStateException: usuário f***@exemplo.com")
                .contains("Suppressed: java.lang.IllegalStateException: Authorization: Bearer ***")
                .doesNotContain("senha=abc")
                .doesNotContain("fulano@exemplo.com")
                .doesNotContain("abc.def");
    }

    @Test
    void preservaOStackTrace() {
        String excecaoRenderizada = renderizar(evento(LOGGER_DA_APLICACAO, new IllegalStateException("falha")));

        assertThat(excecaoRenderizada).contains("at " + ConversorDeExcecaoMascaradaTest.class.getName() + ".preservaOStackTrace");
    }

    @Test
    void eventoSemExcecaoNaoEscreveNada() {
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_DA_APLICACAO)
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("sem exceção"))
                .build();

        assertThat(renderizar(logEvent)).isEmpty();
    }

    /** Pela chave do padrão: o plugin está registrado e o PatternLayout não acrescenta a exceção sem mascaramento. */
    @Test
    void noPatternLayoutAExcecaoSaiSoUmaVezEMascarada() {
        PatternLayout patternLayout = PatternLayout.newBuilder()
                .withConfiguration(new DefaultConfiguration())
                .withPattern("%m%n%xwExMascarado")
                .build();
        IllegalStateException illegalStateException = new IllegalStateException("senha = 'segredo'");

        String linhaDoLog = patternLayout.toSerializable(evento(LOGGER_DA_APLICACAO, illegalStateException));

        assertThat(linhaDoLog)
                .contains("java.lang.IllegalStateException: senha = '***'")
                .doesNotContain("segredo");
        assertThat(linhaDoLog.split("IllegalStateException", -1)).hasSize(2);
    }

    private String renderizar(LogEvent logEvent) {
        StringBuilder excecaoRenderizada = new StringBuilder();
        conversorDeExcecaoMascarada.format(logEvent, excecaoRenderizada);
        return excecaoRenderizada.toString();
    }

    private static LogEvent evento(String nomeDoLogger, Throwable throwable) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(nomeDoLogger)
                .setLevel(Level.ERROR)
                .setMessage(new SimpleMessage("Erro inesperado"))
                .setThrown(throwable)
                .build();
    }
}
