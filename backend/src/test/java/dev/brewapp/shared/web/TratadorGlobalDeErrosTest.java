package dev.brewapp.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.brewapp.shared.CodigoDeErro;
import dev.brewapp.shared.ErroDeNegocio;
import dev.brewapp.shared.TipoDeErro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticação simulada com jwt(): a validação real do token é coberta pelo SegurancaTest.
 * Códigos e tipos escritos por extenso de propósito: o teste fixa o contrato, e renomear uma constante
 * de enum precisa quebrá-lo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TratadorGlobalDeErrosTest.ControladorDeTeste.class)
class TratadorGlobalDeErrosTest {

    private static final String SEGREDO_INTERNO = "select * from adega.evento where senha = 'segredo'";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void erroDeNegocioDeConflitoVira409ComCodeEParams() throws Exception {
        esperarProblema(executar(get("/teste/conflito")), 409, "LOTE_ENVASADO")
                .andExpect(jsonPath("$.type").value("https://brewapp.dev/erros/lote-envasado"))
                .andExpect(jsonPath("$.title").value("Lote já envasado"))
                .andExpect(jsonPath("$.params.loteId").value("0192-lote"));
    }

    @Test
    void erroDeNegocioNaoEncontradoVira404() throws Exception {
        esperarProblema(executar(get("/teste/nao-encontrado")), 404, "LOTE_NAO_ENCONTRADO");
    }

    @Test
    void erroDeNegocioDeRegraVioladaVira422() throws Exception {
        esperarProblema(executar(get("/teste/regra-violada")), 422, "INOCULACAO_DUPLICADA");
    }

    @Test
    void validacaoDoCorpoListaCadaCampoComCodeEParams() throws Exception {
        String corpoInvalido = """
                {"nome": " ", "capacidade": {"valor": -5}}""";

        esperarProblema(executar(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content(corpoInvalido)),
                400, "VALIDACAO_FALHOU")
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.campo == 'nome')].code").value("OBRIGATORIO"))
                .andExpect(jsonPath("$.errors[?(@.campo == 'capacidade.valor')].code").value("DEVE_SER_POSITIVO"));
    }

    @Test
    void validacaoDeParametroTrazOsLimitesNosParams() throws Exception {
        esperarProblema(executar(get("/teste/parametro").param("limite", "500")), 400, "VALIDACAO_FALHOU")
                .andExpect(jsonPath("$.errors[0].campo").value("limite"))
                .andExpect(jsonPath("$.errors[0].code").value("ACIMA_DO_MAXIMO"))
                .andExpect(jsonPath("$.errors[0].params.value").value(200));
    }

    @Test
    void parametroComTipoErradoViraRequisicaoInvalida() throws Exception {
        esperarProblema(executar(get("/teste/parametro").param("limite", "abc")), 400, "REQUISICAO_INVALIDA");
    }

    @Test
    void corpoMalformadoVira400() throws Exception {
        esperarProblema(executar(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{\"nome\": ")),
                400, "CORPO_MALFORMADO");
    }

    @Test
    void contentTypeNaoSuportadoVira415() throws Exception {
        esperarProblema(executar(post("/teste/validacao").contentType(MediaType.TEXT_PLAIN).content("texto")),
                415, "TIPO_DE_CONTEUDO_NAO_SUPORTADO");
    }

    @Test
    void rotaInexistenteVira404() throws Exception {
        esperarProblema(executar(get("/api/v1/rota-que-nao-existe")), 404, "ROTA_NAO_ENCONTRADA");
    }

    @Test
    void metodoNaoSuportadoVira405ComCabecalhoAllow() throws Exception {
        esperarProblema(executar(delete("/teste/conflito")), 405, "METODO_NAO_SUPORTADO")
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")));
    }

    @Test
    void erroInesperadoVira500SemDetalheInterno() throws Exception {
        esperarProblema(executar(get("/teste/erro-inesperado")), 500, "ERRO_INTERNO")
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("segredo"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    private ResultActions executar(MockHttpServletRequestBuilder mockHttpServletRequestBuilder) throws Exception {
        return mockMvc.perform(mockHttpServletRequestBuilder.with(jwt()));
    }

    private static ResultActions esperarProblema(ResultActions resultActions, int status, String code) throws Exception {
        return resultActions
                .andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.params").isMap())
                .andExpect(jsonPath("$.errors").isArray());
    }

    /** Faz o papel do enum de erros de um módulo de negócio. */
    enum ErroDeTeste implements CodigoDeErro {
        LOTE_ENVASADO("Lote já envasado", TipoDeErro.CONFLITO),
        LOTE_NAO_ENCONTRADO("Lote não encontrado", TipoDeErro.NAO_ENCONTRADO),
        INOCULACAO_DUPLICADA("Lote já inoculado", TipoDeErro.REGRA_VIOLADA);

        private final String titulo;
        private final TipoDeErro tipoDeErro;

        ErroDeTeste(String titulo, TipoDeErro tipoDeErro) {
            this.titulo = titulo;
            this.tipoDeErro = tipoDeErro;
        }

        @Override
        public String titulo() {
            return titulo;
        }

        @Override
        public TipoDeErro tipoDeErro() {
            return tipoDeErro;
        }
    }

    record Capacidade(@Positive BigDecimal valor) {
    }

    record CorpoDeTeste(@NotBlank String nome, @Valid Capacidade capacidade) {
    }

    /** Aninhado na classe de teste: fica fora do component scan e só entra neste contexto pelo @Import. */
    @RestController
    @RequestMapping("/teste")
    static class ControladorDeTeste {

        @GetMapping("/conflito")
        void conflito() {
            throw new ErroDeNegocio(ErroDeTeste.LOTE_ENVASADO, Map.of("loteId", "0192-lote"));
        }

        @GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new ErroDeNegocio(ErroDeTeste.LOTE_NAO_ENCONTRADO);
        }

        @GetMapping("/regra-violada")
        void regraViolada() {
            throw new ErroDeNegocio(ErroDeTeste.INOCULACAO_DUPLICADA);
        }

        @PostMapping("/validacao")
        void validacao(@Valid @RequestBody CorpoDeTeste corpoDeTeste) {
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam @Max(200) int limite) {
        }

        @GetMapping("/erro-inesperado")
        void erroInesperado() {
            throw new IllegalStateException(SEGREDO_INTERNO);
        }
    }
}
