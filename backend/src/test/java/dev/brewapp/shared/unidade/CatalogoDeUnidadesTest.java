package dev.brewapp.shared.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import dev.brewapp.shared.unidade.CasosDeTesteCompartilhados.ArquivoDeCasos;
import dev.brewapp.shared.unidade.CasosDeTesteCompartilhados.CasoDeConversao;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Catálogo de unidades (design da API, seção 1.11) contra os mesmos casos do teste TypeScript
 * (packages/shared/src/unidades/catalogo.test.ts), lidos dos mesmos arquivos.
 */
class CatalogoDeUnidadesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosCompartilhados")
    void converteConformeOsCasosCompartilhados(CasoDeConversao casoDeConversao) {
        double valorConvertido = Unidade.valueOf(casoDeConversao.de().unidade())
                .converter(casoDeConversao.de().valor(), Unidade.valueOf(casoDeConversao.para()));

        assertThat(valorConvertido).isCloseTo(casoDeConversao.esperado(), within(casoDeConversao.tolerancia()));
    }

    /** Incluir uma unidade no catálogo exige incluir os seus casos (critério da BREW-45). */
    @Test
    void todaUnidadeDoCatalogoTemCasosCompartilhados() {
        Set<String> unidadesComCasos = CasosDeTesteCompartilhados.carregarTodos().stream()
                .flatMap(arquivoDeCasos -> unidadesDosCasos(arquivoDeCasos.casos()))
                .collect(Collectors.toSet());

        assertThat(unidadesComCasos)
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(Unidade.values()).map(Enum::name).toList());
    }

    @Test
    void casosDeCadaArquivoSaoDaGrandezaDoArquivo() {
        for (ArquivoDeCasos arquivoDeCasos : CasosDeTesteCompartilhados.carregarTodos()) {
            Grandeza grandezaDoArquivo = Grandeza.valueOf(arquivoDeCasos.grandeza());

            assertThat(unidadesDosCasos(arquivoDeCasos.casos()).map(Unidade::valueOf))
                    .as("unidades do arquivo de %s", grandezaDoArquivo)
                    .allSatisfy(unidade -> assertThat(unidade.grandeza()).isEqualTo(grandezaDoArquivo));
        }
    }

    @Test
    void cadaGrandezaTemAUnidadeCanonicaDoDesignDaApi() {
        Map<Grandeza, Unidade> canonicaPorGrandeza = Arrays.stream(Grandeza.values())
                .collect(Collectors.toMap(grandeza -> grandeza, Grandeza::unidadeCanonica));

        assertThat(canonicaPorGrandeza)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        Grandeza.DENSIDADE, Unidade.SG,
                        Grandeza.TEMPERATURA, Unidade.CELSIUS,
                        Grandeza.PRESSAO, Unidade.BAR,
                        Grandeza.VOLUME, Unidade.L,
                        Grandeza.MASSA, Unidade.G));
        assertThat(Arrays.stream(Unidade.values()).filter(Unidade::ehCanonica))
                .as("uma única canônica por grandeza")
                .hasSize(Grandeza.values().length);
    }

    @Test
    void naoConverteEntreGrandezasDiferentes() {
        assertThatThrownBy(() -> Unidade.SG.converter(1.05, Unidade.L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SG")
                .hasMessageContaining("L");
    }

    static List<CasoDeConversao> casosCompartilhados() {
        return CasosDeTesteCompartilhados.carregarTodos().stream()
                .flatMap(arquivoDeCasos -> arquivoDeCasos.casos().stream())
                .toList();
    }

    private static Stream<String> unidadesDosCasos(List<CasoDeConversao> casosDeConversao) {
        return casosDeConversao.stream()
                .flatMap(casoDeConversao -> Stream.of(casoDeConversao.de().unidade(), casoDeConversao.para()));
    }
}
