package dev.brewapp.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonWriter;

/**
 * O JsonWriter daqui imita o aninhamento do ECS do Boot (membro sem nome com o objeto "error"); a configuração real
 * (application.yml + formato ECS) é coberta pelo LogEstruturadoEmProducaoTest.
 */
class MascaramentoDaExcecaoEmJsonTest {

    private static final String LOGGER_DO_JOOQ = "org.jooq.tools.LoggerListener";

    private final JsonWriter<LogEvent> jsonWriter = JsonWriter.of(members -> {
        members.add("message", logEvent -> logEvent.getMessage().getFormattedMessage());
        members.from(LogEvent::getThrown)
                .whenNotNull()
                .usingMembers(thrownMembers -> thrownMembers.add("error").usingMembers(error -> {
                    error.add("type", throwable -> throwable.getClass().getName());
                    error.add("message", Throwable::getMessage);
                    error.add("stack_trace", MascaramentoDaExcecaoEmJsonTest::stackTrace);
                }));
        new MascaramentoDaExcecaoEmJson().customize(members);
    });

    @Test
    void mascaraMensagemEStackTraceDaExcecaoComSegredo() {
        IllegalStateException illegalStateException =
                new IllegalStateException("select * from adega.evento where senha = 'segredo'");

        String json = jsonWriter.writeToString(
                evento("dev.brewapp.qualquer.Classe", "Erro inesperado", illegalStateException));

        assertThat(json)
                .contains("\"type\":\"java.lang.IllegalStateException\"")
                .contains("\"message\":\"select * from adega.evento where senha = '***'\"")
                .contains("java.lang.IllegalStateException: select * from adega.evento where senha = '***'")
                .doesNotContain("segredo");
    }

    @Test
    void mascaraEmailDaExcecaoMesmoNoLoggerComEmailVisivel() {
        RuntimeException runtimeException = new RuntimeException("Key (email)=(fulano@exemplo.com) already exists");

        String json = jsonWriter.writeToString(evento(LOGGER_DO_JOOQ, "Falha no insert", runtimeException));

        assertThat(json)
                .contains("\"message\":\"Key (email)=(f***@exemplo.com) already exists\"")
                .doesNotContain("fulano@exemplo.com");
    }

    @Test
    void mascaraCausaEncadeadaNoStackTrace() {
        IllegalArgumentException causa = new IllegalArgumentException("usuário fulano@exemplo.com com senha=abc");
        RuntimeException runtimeException = new RuntimeException("Falha ao salvar", causa);

        String json =
                jsonWriter.writeToString(evento("dev.brewapp.qualquer.Classe", "Erro inesperado", runtimeException));

        assertThat(json)
                .contains("\"message\":\"Falha ao salvar\"")
                .contains("Caused by: java.lang.IllegalArgumentException: usuário f***@exemplo.com com senha=***")
                .doesNotContain("fulano@exemplo.com")
                .doesNotContain("senha=abc");
    }

    /** A mensagem do evento é da PoliticaDeMascaramento, que já a recebe mascarada; aqui ela não muda. */
    @Test
    void naoMexeEmOutrosCampos() {
        String json = jsonWriter.writeToString(evento(LOGGER_DO_JOOQ, "select 'fulano@exemplo.com'", null));

        assertThat(json).isEqualTo("{\"message\":\"select 'fulano@exemplo.com'\"}");
    }

    private static LogEvent evento(String nomeDoLogger, String mensagem, Throwable throwable) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(nomeDoLogger)
                .setLevel(Level.ERROR)
                .setMessage(new SimpleMessage(mensagem))
                .setThrown(throwable)
                .build();
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
