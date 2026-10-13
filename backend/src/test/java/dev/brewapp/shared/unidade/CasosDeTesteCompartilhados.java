package dev.brewapp.shared.unidade;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lê os arquivos de packages/shared/casos-de-teste (spec 4.5), os mesmos usados pelos testes TypeScript.
 * Formato: { grandeza, referencia, casos: [{ descricao, de: { valor, unidade }, para, esperado, tolerancia }] }.
 */
final class CasosDeTesteCompartilhados {

    private static final String PASTA_NO_CLASSPATH = "/casos-de-teste/";

    private CasosDeTesteCompartilhados() {}

    static ArquivoDeCasos carregar(String nomeDoArquivo) {
        try (InputStream arquivo =
                CasosDeTesteCompartilhados.class.getResourceAsStream(PASTA_NO_CLASSPATH + nomeDoArquivo)) {
            if (arquivo == null) {
                throw new IllegalStateException("Arquivo de casos compartilhados não encontrado: " + nomeDoArquivo);
            }
            return JsonMapper.builder().build().readValue(arquivo, ArquivoDeCasos.class);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    record ArquivoDeCasos(String grandeza, String referencia, List<CasoDeConversao> casos) {}

    /** Converter {@code de} para a unidade {@code para} resulta em {@code esperado}, com até {@code tolerancia} de diferença. */
    record CasoDeConversao(String descricao, ValorInformado de, String para, double esperado, double tolerancia) {

        /** Nome exibido pelo JUnit em cada execução do teste parametrizado. */
        @Override
        public String toString() {
            return "%s: %s %s -> %s".formatted(descricao, de.valor(), de.unidade(), para);
        }
    }

    record ValorInformado(double valor, String unidade) {}
}
