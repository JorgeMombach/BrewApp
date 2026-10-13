package dev.brewapp.shared.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import dev.brewapp.shared.ErroDeNegocio;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuantidadeTest {

    @Test
    void guardaOValorComoFoiInformado() {
        Quantidade quantidade = Quantidade.de(new BigDecimal("12.0"), Unidade.PLATO, Grandeza.DENSIDADE);

        assertThat(quantidade.valor()).isEqualByComparingTo("12.0").hasScaleOf(1);
        assertThat(quantidade.unidade()).isEqualTo(Unidade.PLATO);
        assertThat(quantidade.grandeza()).isEqualTo(Grandeza.DENSIDADE);
    }

    @Test
    void rejeitaUnidadeDeOutraGrandeza() {
        ErroDeNegocio erroDeNegocio = catchThrowableOfType(
                ErroDeNegocio.class, () -> Quantidade.de(new BigDecimal("500"), Unidade.G, Grandeza.VOLUME));

        assertThat(erroDeNegocio.codigoDeErro()).isEqualTo(ErroDeUnidade.UNIDADE_INVALIDA);
        assertThat(erroDeNegocio.params()).isEqualTo(Map.of("unidade", "G", "grandeza", "VOLUME"));
    }

    @Test
    void exigeValorUnidadeEGrandeza() {
        assertThatNullPointerException()
                .isThrownBy(() -> Quantidade.de(null, Unidade.L, Grandeza.VOLUME))
                .withMessage("valor");
        assertThatNullPointerException()
                .isThrownBy(() -> Quantidade.de(BigDecimal.ONE, null, Grandeza.VOLUME))
                .withMessage("unidade");
        assertThatNullPointerException()
                .isThrownBy(() -> Quantidade.de(BigDecimal.ONE, Unidade.L, null))
                .withMessage("grandezaEsperada");
    }

    @Test
    void valorCanonicoNaUnidadeCanonicaEOProprioValor() {
        Quantidade quantidade = Quantidade.de(new BigDecimal("1.0484"), Unidade.SG, Grandeza.DENSIDADE);

        assertThat(quantidade.valorCanonico()).isEqualTo(new BigDecimal("1.0484"));
    }

    /** Sem a escala fixa, o ruído do ponto flutuante (ex.: 1.0484000000000002) chegaria à API e ao banco. */
    @Test
    void valorCanonicoConvertidoTemEscalaFixa() {
        Quantidade quantidade = Quantidade.de(new BigDecimal("12"), Unidade.PLATO, Grandeza.DENSIDADE);

        assertThat(quantidade.valorCanonico()).hasScaleOf(6).isEqualByComparingTo("1.048378");
    }

    @Test
    void valorCanonicoDeUnidadeComDeslocamento() {
        Quantidade quantidade = Quantidade.de(new BigDecimal("64.4"), Unidade.FAHRENHEIT, Grandeza.TEMPERATURA);

        assertThat(quantidade.valorCanonico()).isEqualByComparingTo("18");
    }

    @Test
    void quantidadesIguaisIndependenteDaEscalaDoValor() {
        Quantidade dozePlato = Quantidade.de(new BigDecimal("12"), Unidade.PLATO, Grandeza.DENSIDADE);
        Quantidade dozePlatoComDecimal = Quantidade.de(new BigDecimal("12.00"), Unidade.PLATO, Grandeza.DENSIDADE);

        assertThat(dozePlato).isEqualTo(dozePlatoComDecimal).hasSameHashCodeAs(dozePlatoComDecimal);
    }

    /** Mesma densidade em unidades diferentes não é a mesma quantidade informada. */
    @Test
    void quantidadesComUnidadesDiferentesSaoDiferentes() {
        Quantidade dozePlato = Quantidade.de(new BigDecimal("12"), Unidade.PLATO, Grandeza.DENSIDADE);
        Quantidade dozeBrix = Quantidade.de(new BigDecimal("12"), Unidade.BRIX, Grandeza.DENSIDADE);

        assertThat(dozePlato).isNotEqualTo(dozeBrix);
    }
}
