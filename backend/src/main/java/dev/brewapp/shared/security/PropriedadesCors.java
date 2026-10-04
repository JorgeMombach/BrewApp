package dev.brewapp.shared.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origens aceitas no CORS (spec 9.5), vindas de BREWAPP_CORS_ALLOWED_ORIGINS separadas por vírgula. */
@ConfigurationProperties("brewapp.cors")
record PropriedadesCors(List<String> allowedOrigins) {

    PropriedadesCors {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException("brewapp.cors.allowed-origins precisa ter ao menos uma origem");
        }
        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException("brewapp.cors.allowed-origins não aceita '*': CORS é restrito às origens conhecidas");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
