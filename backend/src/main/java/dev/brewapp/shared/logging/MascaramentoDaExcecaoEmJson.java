package dev.brewapp.shared.logging;

import java.util.Set;
import org.apache.logging.log4j.core.LogEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Exceção do layout JSON (ECS, em logging.structured.json.customizer no application.yml): mascara a mensagem e o stack
 * trace (com as causas encadeadas e suprimidas) já renderizados. O tipo da exceção fica como está.
 * A PoliticaDeMascaramento não alcança a exceção: mensagens de bibliotecas trazem valores (o PostgreSQL inclui o
 * e-mail que violou a chave única; o jOOQ, o SQL). O e-mail é mascarado em todo logger.
 * Os caminhos são os do ECS; outro formato estruturado (Logstash, GELF) nomeia esses campos de outro jeito.
 */
public final class MascaramentoDaExcecaoEmJson implements StructuredLoggingJsonMembersCustomizer<LogEvent> {

    private static final Set<JsonWriter.MemberPath> CAMPOS_DA_EXCECAO =
            Set.of(JsonWriter.MemberPath.of("error.message"), JsonWriter.MemberPath.of("error.stack_trace"));

    @Override
    public void customize(JsonWriter.Members<LogEvent> members) {
        members.applyingValueProcessor(
                ValueProcessor.of(String.class, (String texto) -> MascaradorDeDadosSensiveis.mascarar(texto, true))
                        .whenHasPath(CAMPOS_DA_EXCECAO::contains));
    }
}
