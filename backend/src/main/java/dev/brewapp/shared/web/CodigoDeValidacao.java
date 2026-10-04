package dev.brewapp.shared.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Códigos de cada item de {@code errors} numa falha de validação (design da API, 1.6, tabela
 * "Códigos dos erros de validação"), com as anotações de Bean Validation que geram cada um.
 * Renomear uma constante muda o contrato com os clientes.
 */
public enum CodigoDeValidacao {

    OBRIGATORIO(NotNull.class, NotBlank.class, NotEmpty.class),
    DEVE_SER_POSITIVO(Positive.class),
    NAO_PODE_SER_NEGATIVO(PositiveOrZero.class),
    TAMANHO_INVALIDO(Size.class),
    ABAIXO_DO_MINIMO(Min.class),
    ACIMA_DO_MAXIMO(Max.class),
    FORMATO_INVALIDO(Pattern.class, Email.class),
    /** Valor de tipo errado para o campo (ex.: texto num campo numérico); não vem de anotação. */
    VALOR_INVALIDO;

    private final List<Class<? extends Annotation>> anotacoes;

    @SafeVarargs
    CodigoDeValidacao(Class<? extends Annotation>... anotacoes) {
        this.anotacoes = List.of(anotacoes);
    }

    /** Vazio para anotações sem código próprio: nesse caso o código é o nome da anotação em UPPER_SNAKE_CASE. */
    static Optional<CodigoDeValidacao> daAnotacao(Class<? extends Annotation> tipoDaAnotacao) {
        return Arrays.stream(values())
                .filter(codigoDeValidacao -> codigoDeValidacao.anotacoes.contains(tipoDaAnotacao))
                .findFirst();
    }
}
