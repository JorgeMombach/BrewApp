package dev.brewapp.shared.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.brewapp.shared.unidade.CasosDeTesteCompartilhados.CasoDeConversao;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Mesmos casos do teste TypeScript (packages/shared/src/unidades/densidade.test.ts), lidos do mesmo arquivo. */
class UnidadeDeDensidadeTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosCompartilhados")
    void converteConformeOsCasosCompartilhados(CasoDeConversao casoDeConversao) {
        double valorConvertido = UnidadeDeDensidade.converter(
                casoDeConversao.de().valor(),
                UnidadeDeDensidade.valueOf(casoDeConversao.de().unidade()),
                UnidadeDeDensidade.valueOf(casoDeConversao.para()));

        assertThat(valorConvertido).isCloseTo(casoDeConversao.esperado(), within(casoDeConversao.tolerancia()));
    }

    static List<CasoDeConversao> casosCompartilhados() {
        return CasosDeTesteCompartilhados.carregar("densidade.json").casos();
    }
}
