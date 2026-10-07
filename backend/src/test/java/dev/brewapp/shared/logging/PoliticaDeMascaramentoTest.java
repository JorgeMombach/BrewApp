package dev.brewapp.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.api.Test;

class PoliticaDeMascaramentoTest {

    private static final String LOGGER_DO_JOOQ = "org.jooq.tools.LoggerListener";
    private static final String LOGGER_DA_APLICACAO = "dev.brewapp.qualquer.Classe";

    private final PoliticaDeMascaramento politicaDeMascaramento = PoliticaDeMascaramento.criar(" " + LOGGER_DO_JOOQ + " , ");

    @Test
    void mascaraAMensagemJaFormatadaComOsParametros() {
        LogEvent logEvent = evento(LOGGER_DA_APLICACAO, new ParameterizedMessage("usuário {} com {}", "fulano@exemplo.com", "Bearer abc"));

        LogEvent logEventMascarado = politicaDeMascaramento.rewrite(logEvent);

        assertThat(logEventMascarado.getMessage().getFormattedMessage()).isEqualTo("usuário f***@exemplo.com com Bearer ***");
    }

    @Test
    void mascaraOsValoresDoMdc() {
        SortedArrayStringMap contexto = new SortedArrayStringMap();
        contexto.putValue("correlationId", "rastro-1");
        contexto.putValue("usuario", "fulano@exemplo.com");
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_DA_APLICACAO)
                .setLevel(Level.INFO)
                .setMessage(new ParameterizedMessage("sem dado sensível"))
                .setContextData(contexto)
                .build();

        LogEvent logEventMascarado = politicaDeMascaramento.rewrite(logEvent);

        assertThat(logEventMascarado.getContextData().<String>getValue("usuario")).isEqualTo("f***@exemplo.com");
        assertThat(logEventMascarado.getContextData().<String>getValue("correlationId")).isEqualTo("rastro-1");
    }

    @Test
    void loggerComEmailVisivelAindaMascaraTokens() {
        LogEvent logEvent = evento(LOGGER_DO_JOOQ, new ParameterizedMessage("select 'fulano@exemplo.com', 'Bearer abc'"));

        LogEvent logEventMascarado = politicaDeMascaramento.rewrite(logEvent);

        assertThat(logEventMascarado.getMessage().getFormattedMessage()).isEqualTo("select 'fulano@exemplo.com', 'Bearer ***'");
    }

    @Test
    void semAtributoTodoLoggerTemEmailMascarado() {
        PoliticaDeMascaramento politicaPadrao = PoliticaDeMascaramento.criar(null);
        LogEvent logEvent = evento(LOGGER_DO_JOOQ, new ParameterizedMessage("select 'fulano@exemplo.com'"));

        assertThat(politicaPadrao.rewrite(logEvent).getMessage().getFormattedMessage()).isEqualTo("select 'f***@exemplo.com'");
    }

    @Test
    void eventoSemDadoSensivelEhDevolvidoSemCopia() {
        LogEvent logEvent = evento(LOGGER_DA_APLICACAO, new ParameterizedMessage("Lote {} inoculado", "IPA-2610-A"));

        assertThat(politicaDeMascaramento.rewrite(logEvent)).isSameAs(logEvent);
    }

    @Test
    void preservaNivelLoggerEExcecao() {
        IllegalStateException illegalStateException = new IllegalStateException("falha");
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_DA_APLICACAO)
                .setLevel(Level.ERROR)
                .setMessage(new ParameterizedMessage("senha={}", "abc"))
                .setThrown(illegalStateException)
                .build();

        LogEvent logEventMascarado = politicaDeMascaramento.rewrite(logEvent);

        assertThat(logEventMascarado.getMessage().getFormattedMessage()).isEqualTo("senha=***");
        assertThat(logEventMascarado.getLevel()).isEqualTo(Level.ERROR);
        assertThat(logEventMascarado.getLoggerName()).isEqualTo(LOGGER_DA_APLICACAO);
        assertThat(logEventMascarado.getThrown()).isSameAs(illegalStateException);
    }

    private static LogEvent evento(String nomeDoLogger, ParameterizedMessage parameterizedMessage) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(nomeDoLogger)
                .setLevel(Level.INFO)
                .setMessage(parameterizedMessage)
                .build();
    }
}
