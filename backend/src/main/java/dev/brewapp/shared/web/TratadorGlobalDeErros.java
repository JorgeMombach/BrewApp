package dev.brewapp.shared.web;

import dev.brewapp.shared.ErroDeNegocio;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Ponto único de conversão de exceções em Problem Details (design da API, seção 1.6).
 * Nenhuma resposta leva stack trace, nome de classe, SQL ou mensagem interna: o cliente recebe só
 * o {@code code} e os {@code params}. Os erros de segurança chegam aqui encaminhados pelo filtro
 * (RespostaDeErroDeSeguranca), para manter um formato só.
 */
@RestControllerAdvice
class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    /** Atributos das anotações de Bean Validation que não interessam ao cliente. */
    private static final Set<String> ATRIBUTOS_IGNORADOS = Set.of("message", "groups", "payload");

    @ExceptionHandler(ErroDeNegocio.class)
    ProblemDetail erroDeNegocio(ErroDeNegocio erroDeNegocio) {
        HttpStatus httpStatus = switch (erroDeNegocio.codigoDeErro().tipoDeErro()) {
            case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CONFLITO -> HttpStatus.CONFLICT;
            case REGRA_VIOLADA -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        return Problemas.criar(httpStatus, erroDeNegocio.codigoDeErro(), erroDeNegocio.params());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail naoAutenticado(AuthenticationException authenticationException) {
        return Problemas.criar(HttpStatus.UNAUTHORIZED, ErroGenerico.NAO_AUTENTICADO);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException accessDeniedException) {
        return Problemas.criar(HttpStatus.FORBIDDEN, ErroGenerico.ACESSO_NEGADO);
    }

    /** Qualquer erro não previsto: detalhe completo só no log do servidor. */
    @ExceptionHandler(Exception.class)
    ProblemDetail erroInesperado(Exception exception) {
        LOGGER.error("Erro inesperado ao processar a requisição", exception);
        return Problemas.criar(HttpStatus.INTERNAL_SERVER_ERROR, ErroGenerico.ERRO_INTERNO);
    }

    /**
     * Exceções do próprio Spring MVC (corpo malformado, validação, rota inexistente, método não suportado etc.).
     * O ResponseEntityExceptionHandler já define o status e os cabeçalhos (como o Allow do 405); aqui só
     * trocamos o corpo padrão do Spring pelo formato da API.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders httpHeaders,
                                                             HttpStatusCode httpStatusCode, WebRequest webRequest) {
        ProblemDetail problemDetail = switch (exception) {
            case MethodArgumentNotValidException methodArgumentNotValidException -> Problemas.falhaDeValidacao(
                    httpStatusCode, errosDosCampos(methodArgumentNotValidException.getBindingResult().getFieldErrors()));
            case HandlerMethodValidationException handlerMethodValidationException -> Problemas.falhaDeValidacao(
                    httpStatusCode, errosDosParametros(handlerMethodValidationException));
            case HttpMessageNotReadableException httpMessageNotReadableException ->
                    Problemas.criar(httpStatusCode, ErroGenerico.CORPO_MALFORMADO);
            case NoResourceFoundException noResourceFoundException ->
                    Problemas.criar(httpStatusCode, ErroGenerico.ROTA_NAO_ENCONTRADA);
            case NoHandlerFoundException noHandlerFoundException ->
                    Problemas.criar(httpStatusCode, ErroGenerico.ROTA_NAO_ENCONTRADA);
            case HttpRequestMethodNotSupportedException httpRequestMethodNotSupportedException ->
                    Problemas.criar(httpStatusCode, ErroGenerico.METODO_NAO_SUPORTADO);
            case HttpMediaTypeNotSupportedException httpMediaTypeNotSupportedException ->
                    Problemas.criar(httpStatusCode, ErroGenerico.TIPO_DE_CONTEUDO_NAO_SUPORTADO);
            case HttpMediaTypeNotAcceptableException httpMediaTypeNotAcceptableException ->
                    Problemas.criar(httpStatusCode, ErroGenerico.TIPO_DE_RESPOSTA_NAO_SUPORTADO);
            default -> problemaGenerico(exception, httpStatusCode);
        };
        return super.handleExceptionInternal(exception, problemDetail, httpHeaders, httpStatusCode, webRequest);
    }

    private static ProblemDetail problemaGenerico(Exception exception, HttpStatusCode httpStatusCode) {
        if (httpStatusCode.is5xxServerError()) {
            LOGGER.error("Erro do Spring MVC com status {}", httpStatusCode.value(), exception);
            return Problemas.criar(httpStatusCode, ErroGenerico.ERRO_INTERNO);
        }
        // Parâmetro ausente, tipo inválido na URL, cabeçalho obrigatório faltando etc.
        return Problemas.criar(httpStatusCode, ErroGenerico.REQUISICAO_INVALIDA);
    }

    private static List<Map<String, Object>> errosDosCampos(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(fieldError -> erroDeValidacao(fieldError.getField(), violacaoDoCampo(fieldError)))
                .toList();
    }

    /** Validação de parâmetros do método (@RequestParam, @PathVariable etc.) e de corpos validados em lista. */
    private static List<Map<String, Object>> errosDosParametros(HandlerMethodValidationException handlerMethodValidationException) {
        return handlerMethodValidationException.getParameterValidationResults().stream()
                .flatMap(parameterValidationResult -> parameterValidationResult.getResolvableErrors().stream()
                        .map(messageSourceResolvable -> messageSourceResolvable instanceof FieldError fieldError
                                ? erroDeValidacao(fieldError.getField(), violacaoDoCampo(fieldError))
                                : erroDeValidacao(parameterValidationResult.getMethodParameter().getParameterName(),
                                        violacaoDoParametro(parameterValidationResult, messageSourceResolvable))))
                .toList();
    }

    private static Optional<ConstraintViolation<?>> violacaoDoCampo(FieldError fieldError) {
        return fieldError.contains(ConstraintViolation.class)
                ? Optional.of(fieldError.unwrap(ConstraintViolation.class))
                : Optional.empty();
    }

    private static Optional<ConstraintViolation<?>> violacaoDoParametro(ParameterValidationResult parameterValidationResult,
                                                                       MessageSourceResolvable messageSourceResolvable) {
        try {
            return Optional.of(parameterValidationResult.unwrap(messageSourceResolvable, ConstraintViolation.class));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }

    /** Sem restrição de Bean Validation, o erro é de conversão de tipo (ex.: texto num campo numérico). */
    private static Map<String, Object> erroDeValidacao(String campo, Optional<ConstraintViolation<?>> constraintViolation) {
        String code = constraintViolation
                .map(TratadorGlobalDeErros::codigoDaRestricao)
                .orElse(CodigoDeValidacao.VALOR_INVALIDO.name());
        Map<String, Object> params = constraintViolation
                .<ConstraintDescriptor<?>>map(ConstraintViolation::getConstraintDescriptor)
                .map(TratadorGlobalDeErros::parametrosDaRestricao)
                .orElse(Map.of());
        return Problemas.erroDeValidacao(campo, code, params);
    }

    private static String codigoDaRestricao(ConstraintViolation<?> constraintViolation) {
        Class<? extends Annotation> tipoDaAnotacao =
                constraintViolation.getConstraintDescriptor().getAnnotation().annotationType();
        return CodigoDeValidacao.daAnotacao(tipoDaAnotacao)
                .map(CodigoDeValidacao::name)
                .orElseGet(() -> paraUpperSnakeCase(tipoDaAnotacao.getSimpleName()));
    }

    private static Map<String, Object> parametrosDaRestricao(ConstraintDescriptor<?> constraintDescriptor) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        constraintDescriptor.getAttributes().forEach((nomeDoAtributo, valorDoAtributo) -> {
            if (!ATRIBUTOS_IGNORADOS.contains(nomeDoAtributo)) {
                parametros.put(nomeDoAtributo, valorDoAtributo);
            }
        });
        return parametros;
    }

    /** DecimalMin -> DECIMAL_MIN */
    private static String paraUpperSnakeCase(String nomeDaAnotacao) {
        return nomeDaAnotacao.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}
