package dev.brewapp.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import dev.brewapp.shared.CodigoDeErro;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * O backend não traduz nada: o cliente exibe a tradução do {@code code} (spec, seção 8). Por isso todo código que o
 * backend devolve precisa de uma chave no pt-BR.json do packages/shared, a referência das chaves; a CI das traduções
 * garante que o es e o en têm as mesmas. Um código novo sem tradução quebra este teste, e não a tela do usuário.
 */
class TraducoesDosCodigosDeErroTest {

    private static final Set<String> CHAVES_DE_TRADUCAO = carregarChaves("/i18n/pt-BR.json");

    @Test
    void todoCodigoGenericoTemTraducao() {
        assertThat(CHAVES_DE_TRADUCAO)
                .containsAll(chaves(
                        "erro.",
                        Arrays.stream(ErroGenerico.values()).map(Enum::name).toList()));
    }

    @Test
    void todoCodigoDeValidacaoTemTraducao() {
        assertThat(CHAVES_DE_TRADUCAO)
                .containsAll(chaves(
                        "validacao.",
                        Arrays.stream(CodigoDeValidacao.values())
                                .map(Enum::name)
                                .toList()));
    }

    /** Procura os enums de todos os módulos: um ErroDeLote novo entra na conferência sem mexer neste teste. */
    @Test
    void todoCodigoDeErroDeNegocioTemTraducao() {
        List<String> codigosDeNegocio = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("dev.brewapp").stream()
                        .filter(JavaClass::isEnum)
                        .filter(javaClass -> javaClass.isAssignableTo(CodigoDeErro.class))
                        .flatMap(javaClass -> javaClass.getEnumConstants().stream())
                        .map(JavaEnumConstant::name)
                        .toList();

        // Sem isto, uma falha na busca dos enums faria o teste passar sem conferir nada
        assertThat(codigosDeNegocio).contains("UNIDADE_INVALIDA");
        assertThat(CHAVES_DE_TRADUCAO).containsAll(chaves("erro.", codigosDeNegocio));
    }

    private static List<String> chaves(String prefixo, List<String> codigos) {
        return codigos.stream().map(prefixo::concat).toList();
    }

    private static Set<String> carregarChaves(String caminhoNoClasspath) {
        try (InputStream arquivo = TraducoesDosCodigosDeErroTest.class.getResourceAsStream(caminhoNoClasspath)) {
            if (arquivo == null) {
                throw new IllegalStateException("Arquivo de traduções não encontrado: " + caminhoNoClasspath);
            }
            Map<String, String> traducoes =
                    JsonMapper.builder().build().readValue(arquivo, new TypeReference<Map<String, String>>() {});
            return traducoes.keySet();
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }
}
