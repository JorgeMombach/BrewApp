package dev.brewapp.shared.logging;

import java.util.regex.Pattern;

/**
 * Regras de mascaramento dos logs (DoD, critério 4: nenhum dado pessoal ou token nos logs). Java puro, sem Log4j2,
 * para ser testado isoladamente; a PoliticaDeMascaramento aplica estas regras a cada evento de log.
 * É a segunda linha de defesa: a primeira é não colocar token nem dado pessoal em mensagem de log.
 */
final class MascaradorDeDadosSensiveis {

    static final String MASCARA = "***";

    /** "Bearer <token>" e "Basic <credenciais>", como no cabeçalho Authorization. */
    private static final Pattern ESQUEMA_DE_AUTORIZACAO =
            Pattern.compile("(?i)\\b(bearer|basic)(\\s+)[A-Za-z0-9\\-._~+/]+=*");

    /** JWT solto (cabeçalho e payload em Base64URL começam com "eyJ", que é '{"'). */
    private static final Pattern JWT =
            Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");

    /** Pares chave/valor de segredos, em texto (senha=x), JSON ("password": "x") ou query string (&token=x). */
    private static final Pattern SEGREDO_EM_CHAVE_E_VALOR = Pattern.compile(
            "(?i)\\b(password|senha|secret|client_secret|access_token|refresh_token|id_token|token|api[_-]?key)"
                    + "(\"?\\s*[:=]\\s*\"?)([^\\s\"',;&}]+)");

    /** Mantém a primeira letra e o domínio: fulano@exemplo.com -> f***@exemplo.com, ainda útil para diagnóstico. */
    private static final Pattern EMAIL =
            Pattern.compile("\\b([A-Za-z0-9])[A-Za-z0-9._%+-]*@([A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,})\\b");

    private MascaradorDeDadosSensiveis() {
    }

    /**
     * Tokens e segredos são sempre mascarados. O e-mail pode ficar visível só onde a política permite
     * (SQL do jOOQ em desenvolvimento, para a consulta continuar pronta para copiar e executar).
     */
    static String mascarar(String texto, boolean mascararEmail) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        // Esquema de autorização antes do JWT: "Bearer eyJ..." vira "Bearer ***" em vez de "Bearer " + máscara do JWT
        String mascarado = ESQUEMA_DE_AUTORIZACAO.matcher(texto).replaceAll("$1$2" + MASCARA);
        mascarado = JWT.matcher(mascarado).replaceAll(MASCARA);
        mascarado = SEGREDO_EM_CHAVE_E_VALOR.matcher(mascarado).replaceAll("$1$2" + MASCARA);
        if (mascararEmail) {
            mascarado = EMAIL.matcher(mascarado).replaceAll("$1" + MASCARA + "@$2");
        }
        return mascarado;
    }
}
