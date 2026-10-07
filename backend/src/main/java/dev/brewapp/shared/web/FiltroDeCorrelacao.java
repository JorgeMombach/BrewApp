package dev.brewapp.shared.web;

import dev.brewapp.shared.CabecalhosBrewApp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Identificador de correlação por requisição (design da API, 1.2; spec 7): reaproveita o X-Correlation-Id enviado
 * pelo cliente (inclusive nas sincronizações do celular) ou gera um novo, coloca no MDC e devolve na resposta.
 * Roda antes do Spring Security, para que as respostas 401 e 403 também tenham o identificador.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class FiltroDeCorrelacao extends OncePerRequestFilter {

    /** Chave no MDC: aparece em cada linha de log e no corpo dos erros (Problemas). */
    static final String CHAVE_NO_MDC = "correlationId";

    /** Valor vindo do cliente vai para o log: formato restrito evita injeção de quebra de linha ou texto arbitrário. */
    private static final Pattern FORMATO_ACEITO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = correlationIdDaRequisicao(httpServletRequest);
        MDC.put(CHAVE_NO_MDC, correlationId);
        // Antes da cadeia: a resposta pode ser confirmada (commit) lá dentro, e o cabeçalho precisa ir junto
        httpServletResponse.setHeader(CabecalhosBrewApp.X_CORRELATION_ID, correlationId);
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            // A thread volta para o pool do Tomcat: o identificador não pode vazar para a próxima requisição
            MDC.remove(CHAVE_NO_MDC);
        }
    }

    private static String correlationIdDaRequisicao(HttpServletRequest httpServletRequest) {
        String correlationIdRecebido = httpServletRequest.getHeader(CabecalhosBrewApp.X_CORRELATION_ID);
        if (correlationIdRecebido != null && FORMATO_ACEITO.matcher(correlationIdRecebido).matches()) {
            return correlationIdRecebido;
        }
        return UUID.randomUUID().toString();
    }
}
