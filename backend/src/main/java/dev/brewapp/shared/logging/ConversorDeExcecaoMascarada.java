package dev.brewapp.shared.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.springframework.boot.logging.log4j2.ExtendedWhitespaceThrowablePatternConverter;

/**
 * Exceção do layout de texto ({@code %xwExMascarado} no log4j2-spring.xml): renderiza como o {@code %xwEx} do Spring
 * Boot e mascara o texto inteiro (mensagem, causas encadeadas, suprimidas e stack trace). A PoliticaDeMascaramento
 * não alcança a exceção: mensagens de bibliotecas trazem valores (o PostgreSQL inclui o e-mail que violou a chave
 * única; o jOOQ, o SQL). O e-mail é mascarado em todo logger, inclusive nos que o mantêm visível na mensagem.
 */
@Plugin(name = "ConversorDeExcecaoMascarada", category = PatternConverter.CATEGORY)
@ConverterKeys({"xwExMascarado"})
public final class ConversorDeExcecaoMascarada extends LogEventPatternConverter {

    private final ExtendedWhitespaceThrowablePatternConverter conversorDoSpringBoot;

    private ConversorDeExcecaoMascarada(ExtendedWhitespaceThrowablePatternConverter conversorDoSpringBoot) {
        super("ExcecaoMascarada", "throwable");
        this.conversorDoSpringBoot = conversorDoSpringBoot;
    }

    /** Assinatura exigida pelo Log4j2; as opções são as mesmas do {@code %xwEx} (ex.: {@code %xwExMascarado{short}}). */
    public static ConversorDeExcecaoMascarada newInstance(Configuration configuration, String[] options) {
        return new ConversorDeExcecaoMascarada(ExtendedWhitespaceThrowablePatternConverter.newInstance(configuration, options));
    }

    @Override
    public void format(LogEvent logEvent, StringBuilder buffer) {
        if (logEvent.getThrown() == null) {
            return;
        }
        StringBuilder excecaoRenderizada = new StringBuilder();
        conversorDoSpringBoot.format(logEvent, excecaoRenderizada);
        buffer.append(MascaradorDeDadosSensiveis.mascarar(excecaoRenderizada.toString(), true));
    }

    /** Sem isso, o PatternLayout acrescentaria a exceção de novo, sem mascaramento, ao fim da linha. */
    @Override
    public boolean handlesThrowable() {
        return true;
    }
}
