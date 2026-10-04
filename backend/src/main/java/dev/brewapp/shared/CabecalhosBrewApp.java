package dev.brewapp.shared;

/**
 * Cabeçalhos HTTP próprios da API (design da API, 1.2). Os cabeçalhos padrão vêm das constantes
 * do Spring ({@code org.springframework.http.HttpHeaders}).
 */
public final class CabecalhosBrewApp {

    /** Organização em uso; validada contra os vínculos do usuário. */
    public static final String X_ORGANIZATION_ID = "X-Organization-Id";

    /** Identificador de correlação da requisição; reaproveitado se enviado, gerado pelo servidor se não. */
    public static final String X_CORRELATION_ID = "X-Correlation-Id";

    private CabecalhosBrewApp() {
    }
}
