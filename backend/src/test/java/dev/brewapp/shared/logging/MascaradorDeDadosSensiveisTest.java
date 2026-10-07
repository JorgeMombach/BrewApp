package dev.brewapp.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MascaradorDeDadosSensiveisTest {

    private static final String JWT = JwtFalsoDeTeste.gerar();

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Authorization: Bearer abc.def-ghi_123              | Authorization: Bearer ***
            authorization: bearer abc123==                     | authorization: bearer ***
            Authorization: Basic dXN1YXJpbzpzZW5oYQ==          | Authorization: Basic ***
            senha=minha-senha-123                              | senha=***
            password: segredo                                  | password: ***
            {"client_secret": "abc123", "nome": "x"}           | {"client_secret": "***", "nome": "x"}
            /callback?code=1&access_token=abc&state=2          | /callback?code=1&access_token=***&state=2
            refresh_token=abc; id_token=def                    | refresh_token=***; id_token=***
            API-KEY=chave e apiKey: outra                      | API-KEY=*** e apiKey: ***
            """)
    void mascaraTokensESegredos(String texto, String esperado) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(texto, true)).isEqualTo(esperado);
    }

    @Test
    void jwtSoltoEhMascarado() {
        assertThat(JWT).startsWith("eyJ");

        assertThat(MascaradorDeDadosSensiveis.mascarar("token recebido: " + JWT, true)).isEqualTo("token recebido: ***");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            usuário fulano@exemplo.com criado                 | usuário f***@exemplo.com criado
            maria.silva+teste@sub.empresa.com.br               | m***@sub.empresa.com.br
            Key (email)=(jorge@brewapp.dev) already exists     | Key (email)=(j***@brewapp.dev) already exists
            """)
    void mascaraEmailMantendoAPrimeiraLetraEODominio(String texto, String esperado) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(texto, true)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "insert into organizacao.usuario (email) values ('fulano@exemplo.com')",
            "select 'fulano@exemplo.com'"})
    void emailFicaVisivelQuandoAPoliticaPermite(String texto) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(texto, false)).isEqualTo(texto);
    }

    @ParameterizedTest
    @MethodSource("tokensESegredos")
    void tokensESegredosSaoMascaradosMesmoComEmailVisivel(String texto) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(texto, false)).contains("***").doesNotContain("abc").doesNotContain(JWT);
    }

    private static Stream<String> tokensESegredos() {
        return Stream.of("Bearer " + JWT, JWT, "senha=abc");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Lote IPA-2610-A inoculado em FV-01",
            "Consulta lenta: 623 ms (limite 500 ms): select length(?::text)",
            "token inválido",
            "senha"})
    void textoSemDadoSensivelFicaIgual(String texto) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(texto, true)).isEqualTo(texto);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void textoNuloOuVazioFicaIgual(String texto) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(texto, true)).isEqualTo(texto);
    }
}
