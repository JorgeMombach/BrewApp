package dev.brewapp.shared.unidade;

import dev.brewapp.shared.ErroDeNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * Valor com unidade, guardado como foi informado (spec 3.4: a tela mostra o informado, a comparação usa o canônico).
 * A unidade é sempre da grandeza esperada pelo campo; sinal e faixa do valor dependem do campo e ficam com quem usa.
 */
public final class Quantidade {

    /** Casas do valor canônico convertido: cobre a densidade (SG com 4 a 5 casas) e o mL convertido em litros. */
    private static final int ESCALA_DO_VALOR_CANONICO = 6;

    private final BigDecimal valor;
    private final Unidade unidade;

    private Quantidade(BigDecimal valor, Unidade unidade) {
        this.valor = valor;
        this.unidade = unidade;
    }

    /** Rejeita com {@link ErroDeUnidade#UNIDADE_INVALIDA} a unidade de outra grandeza (ex.: gramas num volume). */
    public static Quantidade de(BigDecimal valor, Unidade unidade, Grandeza grandezaEsperada) {
        Objects.requireNonNull(valor, "valor");
        Objects.requireNonNull(unidade, "unidade");
        Objects.requireNonNull(grandezaEsperada, "grandezaEsperada");
        if (unidade.grandeza() != grandezaEsperada) {
            throw new ErroDeNegocio(
                    ErroDeUnidade.UNIDADE_INVALIDA,
                    Map.of("unidade", unidade.name(), "grandeza", grandezaEsperada.name()));
        }
        return new Quantidade(valor, unidade);
    }

    public BigDecimal valor() {
        return valor;
    }

    public Unidade unidade() {
        return unidade;
    }

    public Grandeza grandeza() {
        return unidade.grandeza();
    }

    /**
     * Valor na unidade canônica da grandeza. Na própria canônica é o valor informado; convertido, sai com escala fixa,
     * sem o ruído do ponto flutuante das fórmulas.
     */
    public BigDecimal valorCanonico() {
        if (unidade.ehCanonica()) {
            return valor;
        }
        return BigDecimal.valueOf(unidade.paraCanonica(valor.doubleValue()))
                .setScale(ESCALA_DO_VALOR_CANONICO, RoundingMode.HALF_EVEN);
    }

    /** Iguais quando o valor numérico e a unidade são iguais: 12 °P e 12,0 °P são a mesma quantidade. */
    @Override
    public boolean equals(Object outroObjeto) {
        return outroObjeto instanceof Quantidade outraQuantidade
                && unidade == outraQuantidade.unidade
                && valor.compareTo(outraQuantidade.valor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor.stripTrailingZeros(), unidade);
    }

    @Override
    public String toString() {
        return valor.toPlainString() + " " + unidade;
    }
}
