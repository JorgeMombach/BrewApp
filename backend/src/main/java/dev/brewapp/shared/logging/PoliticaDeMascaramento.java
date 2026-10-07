package dev.brewapp.shared.logging;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Mascara tokens, segredos e e-mails na mensagem e no MDC de todo evento, antes do layout (texto ou JSON).
 * Usada no log4j2-spring.xml como {@code <PoliticaDeMascaramento loggersComEmailVisivel="..."/>}.
 * O stack trace das exceções não passa por aqui: mensagens de exceção não podem levar dado pessoal.
 */
@Plugin(name = "PoliticaDeMascaramento", category = Core.CATEGORY_NAME, elementType = "rewritePolicy", printObject = true)
public final class PoliticaDeMascaramento implements RewritePolicy {

    /** Loggers cujas mensagens mantêm o e-mail visível (tokens e segredos continuam mascarados). */
    private final Set<String> loggersComEmailVisivel;

    private PoliticaDeMascaramento(Set<String> loggersComEmailVisivel) {
        this.loggersComEmailVisivel = loggersComEmailVisivel;
    }

    /** @param loggersComEmailVisivel nomes completos de loggers, separados por vírgula; vazio por padrão */
    @PluginFactory
    public static PoliticaDeMascaramento criar(@PluginAttribute("loggersComEmailVisivel") String loggersComEmailVisivel) {
        Set<String> nomesDosLoggers = loggersComEmailVisivel == null
                ? Set.of()
                : Arrays.stream(loggersComEmailVisivel.split(","))
                        .map(String::strip)
                        .filter(nomeDoLogger -> !nomeDoLogger.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        return new PoliticaDeMascaramento(nomesDosLoggers);
    }

    @Override
    public LogEvent rewrite(LogEvent logEvent) {
        boolean mascararEmail = !loggersComEmailVisivel.contains(logEvent.getLoggerName());

        String mensagem = logEvent.getMessage().getFormattedMessage();
        String mensagemMascarada = MascaradorDeDadosSensiveis.mascarar(mensagem, mascararEmail);

        StringMap contextoMascarado = new SortedArrayStringMap(logEvent.getContextData());
        // O MDC do SLF4J só guarda texto; outros tipos (ThreadContext com objetos) ficam como estão
        logEvent.getContextData().<Object>forEach((chave, valor) -> {
            if (valor instanceof String texto) {
                contextoMascarado.putValue(chave, MascaradorDeDadosSensiveis.mascarar(texto, mascararEmail));
            }
        });

        boolean mensagemMudou = !mensagemMascarada.equals(mensagem);
        boolean contextoMudou = !contextoMascarado.equals(logEvent.getContextData());
        if (!mensagemMudou && !contextoMudou) {
            return logEvent;
        }
        // O builder copia o evento inteiro (nível, logger, thread, instante, exceção) e troca só o que foi mascarado
        return new Log4jLogEvent.Builder(logEvent)
                .setMessage(mensagemMudou ? new SimpleMessage(mensagemMascarada) : logEvent.getMessage())
                .setContextData(contextoMascarado)
                .build();
    }
}
