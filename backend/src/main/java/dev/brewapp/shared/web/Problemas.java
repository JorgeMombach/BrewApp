package dev.brewapp.shared.web;

import dev.brewapp.shared.CodigoDeErro;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Monta respostas de erro no formato Problem Details (RFC 9457) estendido pelo design da API (seção 1.6).
 * Só aceita códigos vindos dos enums de erro, nunca texto solto: o {@code code} é o contrato com o cliente,
 * e o {@code title} serve só para leitura humana em logs. Os nomes das propriedades do JSON ficam todos aqui.
 */
public final class Problemas {

    private static final String BASE_DO_TIPO = "https://brewapp.dev/erros/";
    private static final String PROPRIEDADE_CODE = "code";
    private static final String PROPRIEDADE_PARAMS = "params";
    private static final String PROPRIEDADE_ERRORS = "errors";
    private static final String PROPRIEDADE_CAMPO = "campo";

    private Problemas() {
    }

    public static ProblemDetail criar(HttpStatusCode httpStatusCode, ErroGenerico erroGenerico) {
        return criar(httpStatusCode, erroGenerico.name(), erroGenerico.titulo(), Map.of());
    }

    public static ProblemDetail criar(HttpStatusCode httpStatusCode, CodigoDeErro codigoDeErro, Map<String, Object> params) {
        return criar(httpStatusCode, codigoDeErro.name(), codigoDeErro.titulo(), params);
    }

    /** Falha de validação (400) com a lista de campos problemáticos em {@code errors}. */
    static ProblemDetail falhaDeValidacao(HttpStatusCode httpStatusCode, List<Map<String, Object>> errosDeValidacao) {
        ProblemDetail problemDetail = criar(httpStatusCode, ErroGenerico.VALIDACAO_FALHOU);
        problemDetail.setProperty(PROPRIEDADE_ERRORS, errosDeValidacao);
        return problemDetail;
    }

    /**
     * Item de {@code errors}: { "campo": "capacidade.valor", "code": "DEVE_SER_POSITIVO", "params": {} }.
     * Recebe o código como texto porque restrições sem código próprio usam o nome da anotação.
     */
    static Map<String, Object> erroDeValidacao(String campo, String code, Map<String, Object> params) {
        Map<String, Object> erroDeValidacao = new LinkedHashMap<>();
        erroDeValidacao.put(PROPRIEDADE_CAMPO, campo);
        erroDeValidacao.put(PROPRIEDADE_CODE, code);
        erroDeValidacao.put(PROPRIEDADE_PARAMS, params);
        return erroDeValidacao;
    }

    private static ProblemDetail criar(HttpStatusCode httpStatusCode, String code, String title, Map<String, Object> params) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(httpStatusCode);
        // LOTE_ENVASADO -> https://brewapp.dev/erros/lote-envasado
        problemDetail.setType(URI.create(BASE_DO_TIPO + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problemDetail.setTitle(title);
        problemDetail.setProperty(PROPRIEDADE_CODE, code);
        problemDetail.setProperty(PROPRIEDADE_PARAMS, params);
        problemDetail.setProperty(PROPRIEDADE_ERRORS, List.of());
        return problemDetail;
    }
}
