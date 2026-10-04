package dev.brewapp.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Os códigos são contrato com os clientes (design da API, seção 1.6). Esta lista espelha as tabelas do
 * documento: incluir, renomear ou remover um código exige atualizar o documento, as traduções dos
 * clientes e este teste juntos.
 */
class ContratoDeCodigosDeErroTest {

    @Test
    void codigosGenericosSaoOsDoDesignDaApi() {
        assertThat(Arrays.stream(ErroGenerico.values()).map(Enum::name)).containsExactlyInAnyOrder(
                "VALIDACAO_FALHOU",
                "CORPO_MALFORMADO",
                "REQUISICAO_INVALIDA",
                "NAO_AUTENTICADO",
                "ACESSO_NEGADO",
                "ROTA_NAO_ENCONTRADA",
                "METODO_NAO_SUPORTADO",
                "TIPO_DE_RESPOSTA_NAO_SUPORTADO",
                "TIPO_DE_CONTEUDO_NAO_SUPORTADO",
                "ERRO_INTERNO");
    }

    @Test
    void codigosDeValidacaoSaoOsDoDesignDaApi() {
        assertThat(Arrays.stream(CodigoDeValidacao.values()).map(Enum::name)).containsExactlyInAnyOrder(
                "OBRIGATORIO",
                "DEVE_SER_POSITIVO",
                "NAO_PODE_SER_NEGATIVO",
                "TAMANHO_INVALIDO",
                "ABAIXO_DO_MINIMO",
                "ACIMA_DO_MAXIMO",
                "FORMATO_INVALIDO",
                "VALOR_INVALIDO");
    }
}
